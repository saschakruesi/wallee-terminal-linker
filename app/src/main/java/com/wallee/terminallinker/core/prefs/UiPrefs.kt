package com.wallee.terminallinker.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallee.terminallinker.core.api.IatUnit
import com.wallee.terminallinker.feature.spaces.SpaceMode
import com.wallee.terminallinker.feature.spaces.SpaceRef
import com.wallee.terminallinker.feature.spaces.SpaceStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.uiDataStore: DataStore<Preferences> by preferencesDataStore(name = "tl.ui")

/** Non-sensitive UI state in DataStore `tl.ui` (docs/01 §Datenhaltung). Credentials never go here. */
class UiPrefs(context: Context) : SpaceStorage {
    private val store = context.applicationContext.uiDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val spaceListSerializer = ListSerializer(SpaceRef.serializer())

    override val activeSpaceId: Flow<Long?> = store.data.map { it[ACTIVE_SPACE_ID] }
    override val recentSpaceIds: Flow<List<Long>> = store.data.map { prefs ->
        prefs[RECENT_SPACE_IDS]?.split(',')?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }
    override val discoveredSpaces: Flow<List<SpaceRef>> = store.data.map { decodeSpaces(it[DISCOVERED_SPACES]) }
    override val manualSpaces: Flow<List<SpaceRef>> = store.data.map { decodeSpaces(it[MANUAL_SPACES]) }
    override val spaceMode: Flow<SpaceMode> = store.data.map { prefs ->
        prefs[SPACE_MODE]?.let { runCatching { SpaceMode.valueOf(it) }.getOrNull() } ?: SpaceMode.AUTO
    }
    override val discoveryTruncated: Flow<Boolean> = store.data.map { it[DISCOVERY_TRUNCATED] ?: false }
    val iatUnit: Flow<IatUnit> = store.data.map { prefs ->
        prefs[IAT_UNIT]?.let { runCatching { IatUnit.valueOf(it) }.getOrNull() } ?: IatUnit.SECONDS
    }
    val showDecommissioned: Flow<Boolean> = store.data.map { it[SHOW_DECOMMISSIONED] ?: false }

    override suspend fun setActiveSpaceId(id: Long?) {
        store.edit { prefs ->
            if (id == null) {
                prefs.remove(ACTIVE_SPACE_ID)
            } else {
                prefs[ACTIVE_SPACE_ID] = id
                val recent = (
                    listOf(id) +
                        (prefs[RECENT_SPACE_IDS]?.split(',')?.mapNotNull { it.toLongOrNull() } ?: emptyList())
                    )
                    .distinct()
                    .take(MAX_RECENT)
                prefs[RECENT_SPACE_IDS] = recent.joinToString(",")
            }
        }
    }

    override suspend fun setDiscoveredSpaces(spaces: List<SpaceRef>) {
        store.edit { it[DISCOVERED_SPACES] = json.encodeToString(spaceListSerializer, spaces) }
    }

    override suspend fun setManualSpaces(spaces: List<SpaceRef>) {
        store.edit { it[MANUAL_SPACES] = json.encodeToString(spaceListSerializer, spaces) }
    }

    override suspend fun setSpaceMode(mode: SpaceMode) {
        store.edit { it[SPACE_MODE] = mode.name }
    }

    override suspend fun setDiscoveryTruncated(truncated: Boolean) {
        store.edit { it[DISCOVERY_TRUNCATED] = truncated }
    }

    suspend fun setIatUnit(unit: IatUnit) {
        store.edit { it[IAT_UNIT] = unit.name }
    }

    suspend fun setShowDecommissioned(show: Boolean) {
        store.edit { it[SHOW_DECOMMISSIONED] = show }
    }

    suspend fun currentIatUnit(): IatUnit = iatUnit.first()

    /** Wipes everything except nothing — used by "delete all local data" together with [CredentialStore.clear]. */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    private fun decodeSpaces(raw: String?): List<SpaceRef> =
        raw?.let { runCatching { json.decodeFromString(spaceListSerializer, it) }.getOrNull() } ?: emptyList()

    private companion object {
        const val MAX_RECENT = 3
        val ACTIVE_SPACE_ID = longPreferencesKey("activeSpaceId")
        val RECENT_SPACE_IDS = stringPreferencesKey("recentSpaceIds")
        val DISCOVERED_SPACES = stringPreferencesKey("discoveredSpaces")
        val MANUAL_SPACES = stringPreferencesKey("manualSpaces")
        val SPACE_MODE = stringPreferencesKey("spaceMode")
        val DISCOVERY_TRUNCATED = booleanPreferencesKey("discoveryTruncated")
        val IAT_UNIT = stringPreferencesKey("iatUnit")
        val SHOW_DECOMMISSIONED = booleanPreferencesKey("showDecommissioned")
    }
}
