package com.wallee.terminallinker.core.api

import java.io.IOException

/**
 * Non-2xx answer from wallee. [status] drives the UI mapping (docs/02 §2); [code], [message] and
 * [errors] come from `RestApiErrorResponse` when the body could be parsed.
 */
class WalleeApiException(
    val status: Int,
    val code: String?,
    override val message: String,
    val errors: Map<String, String> = emptyMap(),
    val errorId: String? = null,
) : IOException(message) {
    val isUnauthorized: Boolean get() = status == 401
    val isForbidden: Boolean get() = status == 403
}

/** Transport failure: no connection, DNS, timeout, TLS. */
class WalleeNetworkException(cause: Throwable) : IOException(cause.message ?: "network error", cause)

/** Thrown when a request needs credentials but none are stored. */
class MissingCredentialsException : IllegalStateException("no credentials")
