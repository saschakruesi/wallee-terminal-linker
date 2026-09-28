package com.wallee.terminallinker.core.auth

/**
 * Single-shot hand-over from the credential scanner to the setup form. The raw QR text and the key never
 * travel through navigation arguments (docs/02 §5); the value is consumed once and discarded.
 */
class CredentialHandoff {
    @Volatile
    private var pending: ScannedCredentials? = null

    fun offer(credentials: ScannedCredentials) {
        pending = credentials
    }

    fun take(): ScannedCredentials? = pending.also { pending = null }
}
