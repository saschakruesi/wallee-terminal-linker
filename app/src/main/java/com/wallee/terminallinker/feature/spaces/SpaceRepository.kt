package com.wallee.terminallinker.feature.spaces

import com.wallee.terminallinker.core.api.IatUnit
import com.wallee.terminallinker.core.api.WalleeApiException
import com.wallee.terminallinker.core.api.WalleeClient
import com.wallee.terminallinker.core.api.dto.ListResponse
import com.wallee.terminallinker.core.api.dto.Space
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

/** What the UI needs from a space; persisted as JSON in DataStore, so it must stay small and stable. */
@Serializable
data class SpaceRef(
    val id: Long,
    val name: String,
    val active: Boolean = true,
    val accountName: String? = null,
    val manual: Boolean = false,
)

/** AUTO: `GET /spaces` lists the spaces; MANUAL: the user entered space IDs by hand (docs/02 §3.1). */
enum class SpaceMode { AUTO, MANUAL }

/** Storage abstraction so the repository is unit-testable without DataStore. */
interface SpaceStorage {
    val activeSpaceId: Flow<Long?>
    val recentSpaceIds: Flow<List<Long>>
    val discoveredSpaces: Flow<List<SpaceRef>>
    val manualSpaces: Flow<List<SpaceRef>>
    val spaceMode: Flow<SpaceMode>

    /** True when `GET /spaces` reported more spaces than the app loads (docs/02 §3.1). */
    val discoveryTruncated: Flow<Boolean>

    suspend fun setActiveSpaceId(id: Long?)

    suspend fun setDiscoveryTruncated(truncated: Boolean)

    suspend fun setDiscoveredSpaces(spaces: List<SpaceRef>)

    suspend fun setManualSpaces(spaces: List<SpaceRef>)

    suspend fun setSpaceMode(mode: SpaceMode)
}

sealed class DiscoveryResult {
    /** `GET /spaces` returned at least one space; the list is stored and the mode is AUTO. */
    data class Found(val spaces: List<SpaceRef>, val hasMore: Boolean) : DiscoveryResult()

    /** Empty list or 403: the app switches to MANUAL mode (docs/02 §3.1, step 2). */
    data class None(val forbidden: Boolean) : DiscoveryResult()
}

/**
 * Unifies automatically discovered and manually added spaces into one list (docs/02 §3.1) and owns the
 * `iat` unit fallback: a 401 on the first discovery is retried once with milliseconds.
 */
class SpaceRepository(
    private val client: WalleeClient,
    private val storage: SpaceStorage,
    private val onIatUnitChanged: suspend (IatUnit) -> Unit = {},
) {
    /** Discovered ∪ manual, sorted by name, without duplicates. */
    val spaces: Flow<List<SpaceRef>> = combine(storage.discoveredSpaces, storage.manualSpaces) { auto, manual ->
        (auto + manual).distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }

    val activeSpace: Flow<SpaceRef?> = combine(spaces, storage.activeSpaceId) { list, id ->
        list.firstOrNull {
            it.id ==
                id
        }
    }

    val recentSpaces: Flow<List<SpaceRef>> = combine(spaces, storage.recentSpaceIds) { list, ids ->
        ids.mapNotNull { id -> list.firstOrNull { it.id == id } }
    }

    val manualSpaces: Flow<List<SpaceRef>> = storage.manualSpaces
    val mode: Flow<SpaceMode> = storage.spaceMode
    val discoveryTruncated: Flow<Boolean> = storage.discoveryTruncated

    /**
     * Connection test: one `GET /spaces?limit=10` without expand. Listing is slow on the wallee side for
     * users with access to many spaces, so the app loads only the first [MAX_SPACES] and lets the user
     * add further spaces by ID (docs/02 §3.1).
     */
    suspend fun discover(): DiscoveryResult {
        val page = try {
            fetchFirstPage()
        } catch (e: WalleeApiException) {
            when {
                e.isUnauthorized && client.iatUnit == IatUnit.SECONDS -> retryWithMillis()
                e.isForbidden -> return switchToManual(forbidden = true)
                else -> throw e
            }
        }
        if (page.data.isEmpty()) return switchToManual(forbidden = false)
        val refs = page.data.map { it.toRef() }
        storage.setDiscoveredSpaces(refs)
        storage.setDiscoveryTruncated(page.hasMore)
        storage.setSpaceMode(SpaceMode.AUTO)
        return DiscoveryResult.Found(refs, page.hasMore)
    }

    private suspend fun retryWithMillis(): ListResponse<Space> {
        client.iatUnit = IatUnit.MILLIS
        return try {
            fetchFirstPage().also { onIatUnitChanged(IatUnit.MILLIS) }
        } catch (e: WalleeApiException) {
            client.iatUnit = IatUnit.SECONDS
            throw e
        }
    }

    private suspend fun fetchFirstPage(): ListResponse<Space> =
        client.get("/spaces", query = listOf("limit" to MAX_SPACES.toString()))

    private suspend fun switchToManual(forbidden: Boolean): DiscoveryResult.None {
        storage.setDiscoveredSpaces(emptyList())
        storage.setDiscoveryTruncated(false)
        storage.setSpaceMode(SpaceMode.MANUAL)
        return DiscoveryResult.None(forbidden)
    }

    /** `GET /spaces/{id}` — verifies a manually entered ID and returns its name. */
    suspend fun verify(id: Long): SpaceRef {
        val space: Space = client.get("/spaces/$id")
        return space.toRef().copy(manual = true)
    }

    suspend fun addManual(space: SpaceRef) {
        val current = storage.manualSpaces.first()
        storage.setManualSpaces(
            (
                current.filter {
                    it.id != space.id
                } + space.copy(manual = true)
                ).sortedBy { it.name.lowercase() },
        )
    }

    suspend fun removeManual(id: Long) {
        storage.setManualSpaces(storage.manualSpaces.first().filter { it.id != id })
        if (storage.activeSpaceId.first() == id) storage.setActiveSpaceId(null)
    }

    suspend fun setActive(id: Long) {
        storage.setActiveSpaceId(id)
    }

    /**
     * Picks the space to show after a successful setup: the preferred one (from a QR code), else the
     * previously active one, else the first active space in the list.
     */
    suspend fun chooseActive(preferredId: Long? = null): SpaceRef? {
        // Discovery order first (as wallee lists them), then manual spaces — not the alphabetical display order.
        val list = (storage.discoveredSpaces.first() + storage.manualSpaces.first()).distinctBy { it.id }
        val previous = storage.activeSpaceId.first()
        val chosen = list.firstOrNull { it.id == preferredId && it.active }
            ?: list.firstOrNull { it.id == previous && it.active }
            ?: list.firstOrNull { it.active }
            ?: return null
        storage.setActiveSpaceId(chosen.id)
        return chosen
    }

    suspend fun clear() {
        storage.setDiscoveredSpaces(emptyList())
        storage.setDiscoveryTruncated(false)
        storage.setManualSpaces(emptyList())
        storage.setActiveSpaceId(null)
        storage.setSpaceMode(SpaceMode.AUTO)
    }

    private fun Space.toRef(): SpaceRef = SpaceRef(
        id = id,
        name = name?.takeIf { it.isNotBlank() } ?: id.toString(),
        active = isActive,
        accountName = account?.name,
    )

    companion object {
        const val MAX_SPACES = 10
    }
}
