package com.wallee.terminallinker.core.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CredentialQrTest {
    // 32-byte key, standard base64 with '+', '/' and padding → must be percent-encoded in the URI form.
    private val key = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
    private val keyWithSpecials = "+/8DBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
    private val encodedSpecials = "%2B%2F8DBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA%3D"

    @Test
    fun `parses uri with spaces and label`() {
        val result = CredentialQr.parse("wallee-app-user://v1?u=12345&k=$key&s=67890,67891&n=Hotel%20Muster")
        val creds = result.getOrThrow()
        assertEquals(12345L, creds.userId)
        assertEquals(key, creds.key)
        assertEquals(listOf(67890L, 67891L), creds.spaceIds)
        assertEquals("Hotel Muster", creds.label)
    }

    @Test
    fun `parses uri without optional parts`() {
        val creds = CredentialQr.parse("wallee-app-user://v1?u=7&k=$key").getOrThrow()
        assertEquals(7L, creds.userId)
        assertTrue(creds.spaceIds.isEmpty())
        assertNull(creds.label)
    }

    @Test
    fun `decodes url-encoded key without turning plus into space`() {
        val creds = CredentialQr.parse("wallee-app-user://v1?u=1&k=$encodedSpecials").getOrThrow()
        assertEquals(keyWithSpecials, creds.key)
        val rawPlus = CredentialQr.parse("wallee-app-user://v1?u=1&k=$keyWithSpecials").getOrThrow()
        assertEquals(keyWithSpecials, rawPlus.key)
    }

    @Test
    fun `parses json form with numeric and string ids`() {
        val creds = CredentialQr.parse(
            """{ "type": "wallee-app-user", "v": 1, "userId": "12345", "key": "$key", "spaceIds": [67890, "67891"], "label": "Hotel Muster" }""",
        ).getOrThrow()
        assertEquals(12345L, creds.userId)
        assertEquals(listOf(67890L, 67891L), creds.spaceIds)
        assertEquals("Hotel Muster", creds.label)
    }

    @Test
    fun `foreign codes are rejected as not wallee`() {
        listOf("https://example.com/x", "2290012345", "", "{\"foo\":1}", "wallee-user://v1?u=1").forEach { raw ->
            val e = CredentialQr.parse(raw).exceptionOrNull()
            assertTrue(e is CredentialQrException.NotAWalleeCode, "expected NotAWalleeCode for '$raw', got $e")
        }
    }

    @Test
    fun `unsupported versions are reported`() {
        val e = CredentialQr.parse("wallee-app-user://v2?u=1&k=$key").exceptionOrNull()
        assertTrue(e is CredentialQrException.UnsupportedVersion)
        val j = CredentialQr.parse("""{"type":"wallee-app-user","v":3,"userId":1,"key":"$key"}""").exceptionOrNull()
        assertTrue(j is CredentialQrException.UnsupportedVersion)
    }

    @Test
    fun `broken or short keys and bad ids are malformed`() {
        assertTrue(
            CredentialQr.parse(
                "wallee-app-user://v1?u=1&k=not*base64",
            ).exceptionOrNull() is CredentialQrException.Malformed,
        )
        assertTrue(
            CredentialQr.parse(
                "wallee-app-user://v1?u=1&k=Q2xhdWRl",
            ).exceptionOrNull() is CredentialQrException.Malformed,
        )
        assertTrue(
            CredentialQr.parse(
                "wallee-app-user://v1?u=abc&k=$key",
            ).exceptionOrNull() is CredentialQrException.Malformed,
        )
        assertTrue(
            CredentialQr.parse("wallee-app-user://v1?u=0&k=$key").exceptionOrNull() is CredentialQrException.Malformed,
        )
        assertTrue(
            CredentialQr.parse("wallee-app-user://v1?k=$key").exceptionOrNull() is CredentialQrException.Malformed,
        )
        assertTrue(
            CredentialQr.parse(
                "wallee-app-user://v1?u=1&k=$key&s=12,x",
            ).exceptionOrNull() is CredentialQrException.Malformed,
        )
    }

    @Test
    fun `label is trimmed and capped, key never appears in toString`() {
        val creds = CredentialQr.parse("wallee-app-user://v1?u=1&k=$key&n=" + "x".repeat(80)).getOrThrow()
        assertEquals(60, creds.label!!.length)
        assertFalse(creds.toString().contains(key))
    }
}
