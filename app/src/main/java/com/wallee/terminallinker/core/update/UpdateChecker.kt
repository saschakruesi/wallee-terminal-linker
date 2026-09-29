package com.wallee.terminallinker.core.update

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** A newer release on GitHub than the running build. */
@Serializable
data class UpdateInfo(val version: String, val url: String)

/** Storage for the update check (DataStore in the app, in-memory in tests). */
interface UpdateStorage {
    val lastUpdateCheckMillis: Flow<Long?>
    val availableUpdate: Flow<UpdateInfo?>
    val dismissedUpdateVersion: Flow<String?>

    suspend fun setLastUpdateCheck(millis: Long, update: UpdateInfo?)

    suspend fun setDismissedUpdateVersion(version: String?)
}

/**
 * In-app update hint (docs/01 §In-App-Update-Hinweis): `GET releases/latest` at most once a day, no auth,
 * compare `tag_name` with the running version. Never downloads anything; the banner links to the page.
 */
class UpdateChecker(
    private val storage: UpdateStorage,
    private val currentVersion: String,
    private val latestReleaseUrl: HttpUrl,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns the newer release if there is one. Uses the stored result when the last check is younger
     * than [MIN_INTERVAL_MILLIS] unless [force]. Network errors and garbled responses are swallowed and keep
     * the last stored result: the hint is best effort and must never disturb the main flow.
     */
    suspend fun check(force: Boolean = false): UpdateInfo? {
        val now = clock()
        val last = storage.lastUpdateCheckMillis.firstOrNull()
        if (!force && last != null && now - last < MIN_INTERVAL_MILLIS) {
            return storage.availableUpdate.firstOrNull()?.takeIf { AppVersion.isNewer(it.version, currentVersion) }
        }
        val fetched = try {
            fetchLatest()
        } catch (e: IOException) {
            return lastResult()
        } catch (e: SerializationException) {
            return lastResult()
        }
        val update = fetched?.takeIf { AppVersion.isNewer(it.version, currentVersion) }
        storage.setLastUpdateCheck(now, update)
        return update
    }

    private suspend fun lastResult(): UpdateInfo? =
        storage.availableUpdate.firstOrNull()?.takeIf { AppVersion.isNewer(it.version, currentVersion) }

    private suspend fun fetchLatest(): UpdateInfo? = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(latestReleaseUrl)
            .header("Accept", "application/vnd.github+json")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val release = json.decodeFromString(Release.serializer(), response.body.string())
            val tag = release.tagName ?: return@withContext null
            UpdateInfo(version = tag.removePrefix("v"), url = release.htmlUrl ?: latestReleaseUrl.toString())
        }
    }

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tagName: String? = null,
        @SerialName("html_url") val htmlUrl: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
    )

    companion object {
        const val MIN_INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000
        const val REPO_OWNER = "saschakruesi"
        const val REPO_NAME = "wallee-terminal-linker"
        const val RELEASES_PAGE = "https://github.com/$REPO_OWNER/$REPO_NAME/releases"
        const val LATEST_RELEASE_API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    }
}
