package com.wallee.terminallinker.core.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Lenient JSON for all wallee payloads: unknown fields are the norm, nulls are omitted. */
val WalleeJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/** `GET /spaces` and other list endpoints (cursor pagination via `after`). */
@Serializable
data class ListResponse<T>(val data: List<T> = emptyList(), val hasMore: Boolean = false, val limit: Int? = null)

/** `GET /payment/terminals/search` (offset pagination). */
@Serializable
data class SearchResponse<T>(
    val data: List<T> = emptyList(),
    val hasMore: Boolean = false,
    val limit: Int? = null,
    val offset: Int? = null,
)

/** Body of any error status, schema `RestApiErrorResponse` in the OpenAPI spec. */
@Serializable
data class RestApiErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val id: String? = null,
    val date: String? = null,
    val errors: Map<String, String> = emptyMap(),
)

@Serializable
data class AccountRef(val id: Long, val name: String? = null, val state: String? = null)

/** Fields of `Space` used by the app (docs/02 §3.1). */
@Serializable
data class Space(
    val id: Long,
    val name: String? = null,
    val state: String? = null,
    val primaryCurrency: String? = null,
    val account: AccountRef? = null,
    val active: Boolean? = null,
    val restrictedActive: Boolean? = null,
) {
    val isActive: Boolean get() = state == "ACTIVE"
}

@Serializable
data class NamedRef(val id: Long, val name: String? = null)

/** `PaymentTerminalType.name` is a localized map (`{"de": "…", "en": "…"}`). */
@Serializable
data class PaymentTerminalType(val id: Long, val name: JsonObject? = null) {
    fun localizedName(language: String): String? {
        val map = name ?: return null
        val key =
            map.keys.firstOrNull {
                it.equals(language, ignoreCase = true) ||
                    it.startsWith("$language-", ignoreCase = true)
            }
                ?: map.keys.firstOrNull { it.startsWith("en", ignoreCase = true) }
                ?: map.keys.firstOrNull()
        return key?.let { runCatching { map.getValue(it).jsonPrimitive.content }.getOrNull() }
    }
}

@Serializable
data class PaymentTerminalLocationVersion(val id: Long, val location: NamedRef? = null)

@Serializable
data class PaymentTerminalConfigurationVersion(val id: Long, val configuration: NamedRef? = null)

/** Fields of `PaymentTerminal` used by the app (docs/02 §3.2). */
@Serializable
data class PaymentTerminal(
    val id: Long,
    val name: String? = null,
    val identifier: String? = null,
    val state: String? = null,
    val deviceSerialNumber: String? = null,
    val deviceName: String? = null,
    val type: PaymentTerminalType? = null,
    val locationVersion: PaymentTerminalLocationVersion? = null,
    val configurationVersion: PaymentTerminalConfigurationVersion? = null,
    val defaultCurrency: String? = null,
    val activatedOn: String? = null,
    val deactivatedOn: String? = null,
    val decommissionedOn: String? = null,
    val activationCode: String? = null,
    val version: Int? = null,
) {
    /** The central criterion of the app: a terminal is linked when a serial number is set. */
    val linked: Boolean get() = !deviceSerialNumber.isNullOrBlank()
}
