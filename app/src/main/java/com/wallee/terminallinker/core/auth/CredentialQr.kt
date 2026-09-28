package com.wallee.terminallinker.core.auth

import com.wallee.terminallinker.core.api.Jwt
import java.net.URLDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Parsed content of a `wallee-app-user://v1…` code (docs/02 §5). The key is kept only as long as needed. */
data class ScannedCredentials(val userId: Long, val key: String, val spaceIds: List<Long>, val label: String?) {
    override fun toString(): String = "ScannedCredentials(userId=$userId, key=****, spaceIds=$spaceIds, label=$label)"
}

sealed class CredentialQrException(message: String) : Exception(message) {
    /** Neither the URI scheme nor the JSON marker: some other QR code. */
    class NotAWalleeCode : CredentialQrException("not a wallee-app-user code")

    /** A newer format version; the app needs an update. */
    class UnsupportedVersion(val version: String) : CredentialQrException("unsupported version $version")

    /** Recognised as a wallee code, but a required field is missing or invalid. */
    class Malformed(detail: String) : CredentialQrException("malformed: $detail")
}

object CredentialQr {
    const val SCHEME = "wallee-app-user://"
    const val JSON_TYPE = "wallee-app-user"
    const val SUPPORTED_VERSION = 1
    private const val MIN_KEY_BYTES = 16
    private const val MAX_LABEL_LENGTH = 60

    /** Never logs the raw value. */
    fun parse(raw: String): Result<ScannedCredentials> = runCatching {
        val text = raw.trim()
        when {
            text.startsWith(SCHEME, ignoreCase = true) -> parseUri(text.substring(SCHEME.length))
            text.startsWith("{") -> parseJson(text)
            else -> throw CredentialQrException.NotAWalleeCode()
        }
    }

    private fun parseUri(rest: String): ScannedCredentials {
        val questionMark = rest.indexOf('?')
        val versionPart = (if (questionMark >= 0) rest.substring(0, questionMark) else rest).trim('/')
        val version = versionPart.removePrefix("v").removePrefix("V")
        if (version.toIntOrNull() != SUPPORTED_VERSION) throw CredentialQrException.UnsupportedVersion(versionPart)
        val query = if (questionMark >= 0) rest.substring(questionMark + 1) else ""
        val params = query.split('&').filter { it.isNotBlank() }.associate { pair ->
            val eq = pair.indexOf('=')
            if (eq < 0) pair to "" else pair.substring(0, eq) to pair.substring(eq + 1)
        }
        val userId = params["u"]?.let(::decodePercent)?.toLongOrNull()
            ?.takeIf { it > 0 } ?: throw CredentialQrException.Malformed("u")
        val key = params["k"]?.let(::decodePercent)?.takeIf(::isValidKey) ?: throw CredentialQrException.Malformed("k")
        val spaceIds = params["s"]?.let(::decodePercent)?.let(::parseSpaceIds) ?: emptyList()
        val label = params["n"]?.let { URLDecoder.decode(it, "UTF-8") }?.let(::normalizeLabel)
        return ScannedCredentials(userId, key, spaceIds, label)
    }

    private fun parseJson(text: String): ScannedCredentials {
        val obj: JsonObject = runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw CredentialQrException.NotAWalleeCode() }
        if (obj["type"]?.asString() != JSON_TYPE) throw CredentialQrException.NotAWalleeCode()
        val version = obj["v"]?.jsonPrimitiveOrNull()?.intOrNull ?: obj["v"]?.asString()?.toIntOrNull()
        if (version != SUPPORTED_VERSION) throw CredentialQrException.UnsupportedVersion(obj["v"]?.asString() ?: "?")
        val userId = obj["userId"]?.jsonPrimitiveOrNull()?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }
            ?.takeIf { it > 0 } ?: throw CredentialQrException.Malformed("userId")
        val key = obj["key"]?.asString()?.takeIf(::isValidKey) ?: throw CredentialQrException.Malformed("key")
        val spaceIds = when (val ids = obj["spaceIds"]) {
            null -> emptyList()

            is JsonArray -> ids.jsonArray.mapNotNull {
                it.jsonPrimitiveOrNull()?.let { p ->
                    p.longOrNull
                        ?: p.contentOrNull?.toLongOrNull()
                }
            }

            else -> ids.asString()?.let(::parseSpaceIds) ?: emptyList()
        }
        val label = obj["label"]?.asString()?.let(::normalizeLabel)
        return ScannedCredentials(userId, key, spaceIds, label)
    }

    private fun parseSpaceIds(value: String): List<Long> {
        if (value.isBlank()) return emptyList()
        return value.split(',').map { part ->
            part.trim().toLongOrNull()?.takeIf { it > 0 } ?: throw CredentialQrException.Malformed("s")
        }.distinct()
    }

    private fun isValidKey(key: String): Boolean {
        if (key.isBlank() || key.any { it.isWhitespace() }) return false
        val bytes = runCatching { Jwt.decodeKey(key) }.getOrNull() ?: return false
        return bytes.size >= MIN_KEY_BYTES
    }

    private fun normalizeLabel(label: String): String? = label.trim().take(MAX_LABEL_LENGTH).takeIf { it.isNotEmpty() }

    /** Percent-decoding without treating `+` as space (the key may legitimately contain `+`). */
    private fun decodePercent(value: String): String = URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

    private fun kotlinx.serialization.json.JsonElement.asString(): String? = (this as? JsonPrimitive)?.contentOrNull
}
