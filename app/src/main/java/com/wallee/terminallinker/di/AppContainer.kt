package com.wallee.terminallinker.di

import android.content.Context
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.core.api.DebugLoggingInterceptor
import com.wallee.terminallinker.core.api.WalleeClient
import com.wallee.terminallinker.core.auth.CredentialHandoff
import com.wallee.terminallinker.core.auth.CredentialStore
import com.wallee.terminallinker.core.prefs.UiPrefs
import com.wallee.terminallinker.feature.spaces.SpaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container (docs/01 §DI): one instance per process, created in [com.wallee.terminallinker.TerminalLinkerApp]. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val credentialStore: CredentialStore by lazy { CredentialStore(appContext) }
    val uiPrefs: UiPrefs by lazy { UiPrefs(appContext) }
    val credentialHandoff = CredentialHandoff()

    val walleeClient: WalleeClient by lazy {
        val interceptors = if (BuildConfig.DEBUG) arrayOf(DebugLoggingInterceptor()) else emptyArray()
        WalleeClient(
            credentialsProvider = { credentialStore.load() },
            httpClient = WalleeClient.defaultHttpClient(*interceptors),
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

    /** "Alle lokalen Daten löschen": credentials, spaces, preferences. */
    suspend fun wipe() {
        credentialStore.clear()
        spaceRepository.clear()
        uiPrefs.clearAll()
    }
}
