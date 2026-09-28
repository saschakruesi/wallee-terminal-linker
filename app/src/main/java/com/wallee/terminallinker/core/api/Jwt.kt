package com.wallee.terminallinker.core.api

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Unit of the `iat` claim. wallee expects seconds; [MILLIS] is the one-time fallback tried on a 401 (docs/02 §1). */
enum class IatUnit { SECONDS, MILLIS }

/**
 * Per-request HS256 token bound to method and path (docs/02 §1). No JWT library: header and payload are
 * compact JSON, base64url without padding, signed with HMAC-SHA256 over the base64-decoded authentication key.
 */
object Jwt {
    private val json = Json
    private val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    private val headerSegment: String by lazy {
        encode(
            json.encodeToString(
                JsonObject.serializer(),
                JsonObject(
                    mapOf(
                        "alg" to JsonPrimitive("HS256"),
                        "typ" to JsonPrimitive("JWT"),
                        "ver" to JsonPrimitive(1),
                    ),
                ),
            ).toByteArray(),
        )
    }

    fun create(
        userId: Long,
        authenticationKey: String,
        method: String,
        requestPath: String,
        nowMillis: Long = System.currentTimeMillis(),
        iatUnit: IatUnit = IatUnit.SECONDS,
    ): String {
        val iat = if (iatUnit == IatUnit.SECONDS) nowMillis / 1000 else nowMillis
        val payload = JsonObject(
            mapOf(
                "sub" to JsonPrimitive(userId.toString()),
                "iat" to JsonPrimitive(iat),
                "requestPath" to JsonPrimitive(requestPath),
                "requestMethod" to JsonPrimitive(method.uppercase()),
            ),
        )
        val signingInput =
            headerSegment + "." + encode(json.encodeToString(JsonObject.serializer(), payload).toByteArray())
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(decodeKey(authenticationKey), "HmacSHA256"))
        val signature = mac.doFinal(signingInput.toByteArray())
        return signingInput + "." + encode(signature)
    }

    /** The backend shows the key as base64; accept standard and url-safe alphabets, with or without padding. */
    fun decodeKey(authenticationKey: String): ByteArray {
        val trimmed = authenticationKey.trim()
        return try {
            Base64.getDecoder().decode(trimmed)
        } catch (e: IllegalArgumentException) {
            Base64.getUrlDecoder().decode(trimmed.trimEnd('='))
        }
    }

    private fun encode(bytes: ByteArray): String = urlEncoder.encodeToString(bytes)
}
