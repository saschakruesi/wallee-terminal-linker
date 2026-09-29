package com.wallee.terminallinker.di

import android.content.Context
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.core.api.DebugLoggingInterceptor
import com.wallee.terminallinker.core.api.WalleeClient
import com.wallee.terminallinker.core.auth.CredentialHandoff
import com.wallee.terminallinker.core.auth.CredentialStore
import com.wallee.terminallinker.core.prefs.LocalePrefs
import com.wallee.terminallinker.core.prefs.UiPrefs
import com.wallee.terminallinker.core.update.UpdateChecker
import com.wallee.terminallinker.feature.spaces.SpaceRepository
import com.wallee.terminallinker.feature.terminals.TerminalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Manual dependency container (docs/01 §DI): one instance per process, created in [com.wallee.terminallinker.TerminalLinkerApp]. */
class AppContainer(context: Context, private val walleeBaseUrl: String? = null) {
    private val baseContext: Context = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val localePrefs: LocalePrefs by lazy { LocalePrefs(baseContext) }

    /** Application context whose resources follow the language chosen in the settings (for ViewModel texts). */
    val appContext: Context get() = localePrefs.wrap(baseContext)

    val credentialStore: CredentialStore by lazy { CredentialStore(baseContext) }
    val uiPrefs: UiPrefs by lazy { UiPrefs(baseContext) }
    val credentialHandoff = CredentialHandoff()

    val walleeClient: WalleeClient by lazy {
        val interceptors = if (BuildConfig.DEBUG) arrayOf(DebugLoggingInterceptor()) else emptyArray()
        WalleeClient(
            credentialsProvider = { credentialStore.load() },
            httpClient = WalleeClient.defaultHttpClient(*interceptors),
            // Debug builds can point at a local mock (BuildConfig.WALLEE_BASE_URL); release always uses app-wallee.com.
            baseUrl = baseUrl().toHttpUrl(),
        ).also { client ->
            appScope.launch { client.iatUnit = uiPrefs.currentIatUnit() }
        }
    }

    val spaceRepository: SpaceRepository by lazy {
        SpaceRepository(
            client = walleeClient,
            storage = uiPrefs,
            onIatUnitChanged = { uiPrefs.setIatUnit(it) },
        )
    }

    val terminalRepository: TerminalRepository by lazy { TerminalRepository(walleeClient) }

    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(
            storage = uiPrefs,
            currentVersion = BuildConfig.VERSION_NAME,
            latestReleaseUrl = UpdateChecker.LATEST_RELEASE_API.toHttpUrl(),
        )
    }

    private fun baseUrl(): String = walleeBaseUrl
        ?: if (BuildConfig.DEBUG) BuildConfig.WALLEE_BASE_URL else WalleeClient.DEFAULT_BASE_URL

    /** "Alle lokalen Daten löschen": credentials, spaces, preferences, caches. */
    suspend fun wipe() {
        credentialStore.clear()
        spaceRepository.clear()
        terminalRepository.clear()
        uiPrefs.clearAll()
        localePrefs.clear()
    }
}
