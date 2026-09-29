package com.wallee.terminallinker.core.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.wallee.terminallinker.BuildConfig
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

    is WalleeNetworkException ->
        if (context.isOnline()) context.getString(R.string.error_network) else context.getString(R.string.error_offline)

    is MissingCredentialsException -> context.getString(R.string.error_no_credentials)

    is CredentialQrException.NotAWalleeCode -> context.getString(R.string.qr_error_not_wallee)

    is CredentialQrException.UnsupportedVersion -> context.getString(R.string.qr_error_version)

    is CredentialQrException.Malformed -> context.getString(R.string.qr_error_malformed)

    else -> {
        if (BuildConfig.DEBUG) Log.w("wallee-error", "unmapped ${this.javaClass.name}: $message", this)
        context.getString(R.string.error_unknown) + " (" + this.javaClass.simpleName + ")"
    }
}

/** True when the device currently has a network with internet capability (flight mode → false). */
fun Context.isOnline(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
