package com.wallee.terminallinker.core.api

import android.content.Context
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.auth.CredentialQrException

/** Maps client exceptions onto the user-facing texts from docs/02 §2. Never includes the token or key. */
fun Throwable.toUserMessage(context: Context): String = when (this) {
    is WalleeApiException -> when (status) {
        401 -> context.getString(R.string.error_401)

        403 -> context.getString(R.string.error_403)

        404 -> context.getString(R.string.error_404)

        409 -> context.getString(R.string.error_409)

        422 -> message.ifBlank { context.getString(R.string.error_generic, status) }

        429 -> context.getString(R.string.error_429)

        else -> context.getString(R.string.error_generic, status) +
            if (message.startsWith("HTTP")) "" else " – $message"
    }

    is WalleeNetworkException -> context.getString(R.string.error_network)

    is MissingCredentialsException -> context.getString(R.string.error_no_credentials)

    is CredentialQrException.NotAWalleeCode -> context.getString(R.string.qr_error_not_wallee)

    is CredentialQrException.UnsupportedVersion -> context.getString(R.string.qr_error_version)

    is CredentialQrException.Malformed -> context.getString(R.string.qr_error_malformed)

    else -> message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.error_unknown)
}
