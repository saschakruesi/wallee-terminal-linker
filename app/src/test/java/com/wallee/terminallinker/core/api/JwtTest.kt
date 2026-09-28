package com.wallee.terminallinker.core.api

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class JwtTest {
    // 32-byte key 0x01..0x20, base64. Vector computed independently with Python hmac/hashlib
    // (see docs/02 §1; cross-check against the Virtual Terminal's jwt.test.ts when available).
    private val key = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
    private val expected =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsInZlciI6MX0." +
            "eyJzdWIiOiIxMjM0NSIsImlhdCI6MTcwMDAwMDAwMCwicmVxdWVzdFBhdGgiOiIvYXBpL3YyLjAvc3BhY2VzP2xpbWl0PTEwMCIs" +
            "InJlcXVlc3RNZXRob2QiOiJHRVQifQ." +
            "AXRGgwzkPgmaJOIB88ylj7fgS7Rcj1BXfr_uAiO43Ao"

    @Test
    fun `matches the known vector`() {
        val token = Jwt.create(
            userId = 12345,
            authenticationKey = key,
            method = "get",
            requestPath = "/api/v2.0/spaces?limit=100",
            nowMillis = 1_700_000_000_123,
            iatUnit = IatUnit.SECONDS,
        )
        assertEquals(expected, token)
    }

    @Test
    fun `payload carries sub as string, iat in seconds, method upper case`() {
        val token = Jwt.create(
            12345,
            key,
            "post",
            "/api/v2.0/payment/terminals/1/link?serialNumber=A%2B1",
            1_700_000_000_999,
        )
        val parts = token.split('.')
        assertEquals(3, parts.size)
        val header = Json.parseToJsonElement(decode(parts[0])).jsonObject
        assertEquals("HS256", header["alg"]!!.jsonPrimitive.content)
        assertEquals("JWT", header["typ"]!!.jsonPrimitive.content)
        assertEquals("1", header["ver"]!!.jsonPrimitive.content)
        val payload = Json.parseToJsonElement(decode(parts[1])).jsonObject
        assertEquals("12345", payload["sub"]!!.jsonPrimitive.content)
        assertEquals("1700000000", payload["iat"]!!.jsonPrimitive.content)
        assertEquals("POST", payload["requestMethod"]!!.jsonPrimitive.content)
        assertEquals(
            "/api/v2.0/payment/terminals/1/link?serialNumber=A%2B1",
            payload["requestPath"]!!.jsonPrimitive.content,
        )
        assertFalse(token.contains('='))
        assertFalse(token.contains('+'))
        assertFalse(token.contains('/'))
    }

    @Test
    fun `millis mode changes iat and therefore the signature`() {
        val seconds = Jwt.create(1, key, "GET", "/api/v2.0/spaces", 1_700_000_000_000, IatUnit.SECONDS)
        val millis = Jwt.create(1, key, "GET", "/api/v2.0/spaces", 1_700_000_000_000, IatUnit.MILLIS)
        assertNotEquals(seconds, millis)
        val payload = Json.parseToJsonElement(decode(millis.split('.')[1])).jsonObject
        assertEquals("1700000000000", payload["iat"]!!.jsonPrimitive.content)
    }

    @Test
    fun `accepts url-safe keys without padding`() {
        val urlSafe = key.replace('+', '-').replace('/', '_').trimEnd('=')
        assertEquals(Jwt.decodeKey(key).toList(), Jwt.decodeKey(urlSafe).toList())
    }

    private fun decode(segment: String): String = String(Base64.getUrlDecoder().decode(segment))
}
