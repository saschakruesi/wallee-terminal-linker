package com.wallee.terminallinker.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Application User ID plus authentication key. Never logged, never serialized into routes. */
data class Credentials(val userId: Long, val authenticationKey: String) {
    override fun toString(): String = "Credentials(userId=$userId, authenticationKey=****)"
}

/**
 * Keeps the one credential set of this installation in `EncryptedSharedPreferences` (`tl.credentials`,
 * Android Keystore) — or only in memory when the user declined "remember on this device" (docs/01 §Datenhaltung).
 */
class CredentialStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Volatile
    private var memoryOnly: Credentials? = null

    private val state = MutableStateFlow<Credentials?>(null)
    private var loaded = false

    /** Current credentials, memory-only set first, then the encrypted store. */
    val credentials: StateFlow<Credentials?>
        get() {
            ensureLoaded()
            return state
        }

    fun load(): Credentials? {
        ensureLoaded()
        return state.value
    }

    fun isPersisted(): Boolean = prefs.contains(KEY_USER_ID)

    fun save(credentials: Credentials, persist: Boolean) {
        if (persist) {
            prefs.edit {
                putLong(KEY_USER_ID, credentials.userId)
                putString(KEY_AUTH_KEY, credentials.authenticationKey)
            }
            memoryOnly = null
        } else {
            prefs.edit { clear() }
            memoryOnly = credentials
        }
        state.value = credentials
    }

    fun clear() {
        prefs.edit { clear() }
        memoryOnly = null
        state.value = null
    }

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        state.value = memoryOnly ?: readPersisted()
    }

    private fun readPersisted(): Credentials? {
        if (!prefs.contains(KEY_USER_ID)) return null
        val key = prefs.getString(KEY_AUTH_KEY, null) ?: return null
        return Credentials(prefs.getLong(KEY_USER_ID, 0L), key)
    }

    private companion object {
        const val FILE_NAME = "tl.credentials"
        const val KEY_USER_ID = "userId"
        const val KEY_AUTH_KEY = "authKey"
    }
}
