package com.wallee.terminallinker.core.serial

/**
 * Normalises a scanned barcode/QR payload into a device serial number (docs/02 §4).
 * Pure Kotlin so it is unit-testable without the camera or the API.
 */
object SerialNumber {
    private val VALID = Regex("^[A-Za-z0-9-]{6,32}$")

    /** `SN:`, `S/N:`, `SERIAL`, `SERIAL NO`, `SERIAL NUMBER` followed by an optional separator. */
    private val PREFIX = Regex("(?i)(?:\\bS/N\\b|\\bSN\\b|\\bSERIAL(?:\\s*(?:NO|NUMBER))?)\\s*[:=#.]?\\s*")
    private val TOKEN_END = Regex("[;,\\s|]")
    private val URL_TOKEN = Regex("^[A-Z0-9-]{8,20}$")

    sealed class Error(message: String) : Exception(message) {
        class Empty : Error("empty")

        class Invalid(val raw: String) : Error("not a serial number")
    }

    fun parse(raw: String): Result<String> {
        val text = raw.trim()
        if (text.isEmpty()) return Result.failure(Error.Empty())
        extractAfterPrefix(text)?.let { return validate(it, raw) }
        if (isUrl(text)) {
            extractFromUrl(text)?.let { return validate(it, raw) }
            return Result.failure(Error.Invalid(raw))
        }
        return validate(text.filterNot { it.isWhitespace() }, raw)
    }

    private fun extractAfterPrefix(text: String): String? {
        val match = PREFIX.find(text) ?: return null
        val rest = text.substring(match.range.last + 1)
        val end = TOKEN_END.find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end).takeIf { it.isNotEmpty() }
    }

    private fun isUrl(text: String): Boolean =
        text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)

    /** Last path or query component that looks like a serial number. */
    private fun extractFromUrl(text: String): String? = text.substringAfter("://")
        .split('/', '?', '&', '=', '#')
        .map { it.trim() }
        .lastOrNull { URL_TOKEN.matches(it.uppercase()) }

    private fun validate(candidate: String, raw: String): Result<String> =
        if (VALID.matches(candidate)) Result.success(candidate) else Result.failure(Error.Invalid(raw))
}
