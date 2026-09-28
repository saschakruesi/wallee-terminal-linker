package com.wallee.terminallinker.core.api

import com.wallee.terminallinker.core.api.dto.RestApiErrorResponse
import com.wallee.terminallinker.core.api.dto.WalleeJson
import com.wallee.terminallinker.core.auth.Credentials
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Result of a raw call: status plus body text (empty for 204). */
class ApiResponse(val status: Int, val body: String)

/**
 * Generic wallee v2.0 client (docs/02 §2). Builds the URL with [HttpUrl.Builder], signs exactly
 * `encodedPath?encodedQuery`, sends `Authorization: Bearer <jwt>` and the `space` header, maps errors.
 * No automatic retries: link/unlink are not idempotent from the user's point of view.
 */
class WalleeClient(
    private val credentialsProvider: () -> Credentials?,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val json: Json = WalleeJson,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Mutable so the setup flow can switch to milliseconds after a 401 (docs/02 §1). */
    @Volatile
    var iatUnit: IatUnit = IatUnit.SECONDS

    /** Builds the exact request URL; exposed for tests and for signing. */
    fun buildUrl(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        expand: List<String> = emptyList(),
    ): HttpUrl {
        val builder = baseUrl.newBuilder().encodedPath(API_PREFIX + path)
        query.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        expand.forEach { builder.addQueryParameter("expand", it) }
        return builder.build()
    }

    suspend fun raw(
        method: String,
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        expand: List<String> = emptyList(),
        spaceId: Long? = null,
        jsonBody: String? = null,
    ): ApiResponse {
        val credentials = credentialsProvider() ?: throw MissingCredentialsException()
        val url = buildUrl(path, query, expand)
        val requestPath = url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: "")
        val token = Jwt.create(
            userId = credentials.userId,
            authenticationKey = credentials.authenticationKey,
            method = method,
            requestPath = requestPath,
            nowMillis = clock(),
            iatUnit = iatUnit,
        )
        val upper = method.uppercase()
        // OkHttp requires a body for POST/PUT/PATCH; link/unlink send none, so an empty body is used.
        val body = jsonBody?.toRequestBody(JSON_MEDIA_TYPE)
            ?: if (upper == "POST" || upper == "PUT" || upper == "PATCH") EMPTY_BODY else null
        val request = Request.Builder()
            .url(url)
            .method(upper, body)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .apply { if (spaceId != null) header("space", spaceId.toString()) }
            .build()
        val response = try {
            httpClient.newCall(request).await()
        } catch (e: IOException) {
            throw WalleeNetworkException(e)
        }
        response.use {
            val body = it.body.string()
            if (!it.isSuccessful) throw toApiException(it.code, body)
            return ApiResponse(it.code, body)
        }
    }

    suspend inline fun <reified T> get(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        expand: List<String> = emptyList(),
        spaceId: Long? = null,
    ): T = decode(raw("GET", path, query, expand, spaceId).body)

    suspend inline fun <reified T> post(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        expand: List<String> = emptyList(),
        spaceId: Long? = null,
        jsonBody: String? = null,
    ): T = decode(raw("POST", path, query, expand, spaceId, jsonBody).body)

    /** For 204 endpoints (link/unlink). */
    suspend fun postNoContent(path: String, query: List<Pair<String, String>> = emptyList(), spaceId: Long? = null) {
        raw("POST", path, query, emptyList(), spaceId)
    }

    inline fun <reified T> decode(body: String): T = decodeWith(body, kotlinx.serialization.serializer<T>())

    fun <T> decodeWith(body: String, serializer: kotlinx.serialization.KSerializer<T>): T =
        json.decodeFromString(serializer, body)

    private fun toApiException(status: Int, body: String): WalleeApiException {
        val parsed = runCatching { json.decodeFromString(RestApiErrorResponse.serializer(), body) }.getOrNull()
        return WalleeApiException(
            status = status,
            code = parsed?.code,
            message = parsed?.message?.takeIf { it.isNotBlank() } ?: "HTTP $status",
            errors = parsed?.errors ?: emptyMap(),
            errorId = parsed?.id,
        )
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://app-wallee.com"
        const val API_PREFIX = "/api/v2.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)

        fun defaultHttpClient(vararg interceptors: okhttp3.Interceptor): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .apply { interceptors.forEach { addInterceptor(it) } }
            .build()
    }
}

/** Cancellable await over OkHttp's enqueue: cancelling the coroutine cancels the call. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        },
    )
    continuation.invokeOnCancellation { cancel() }
}
