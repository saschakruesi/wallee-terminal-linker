package com.wallee.terminallinker.core.serial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SerialNumberTest {
    private fun ok(raw: String): String = SerialNumber.parse(raw).getOrThrow()

    private fun fails(raw: String) = assertTrue(SerialNumber.parse(raw).isFailure, "expected failure for '$raw'")

    @Test
    fun `plain code128 values pass through, whitespace is removed`() {
        assertEquals("2290012345", ok("2290012345"))
        assertEquals("2290012345", ok("  2290 0123 45\n"))
        assertEquals("A77-0123456", ok("A77-0123456"))
    }

    @Test
    fun `prefixed values take the token after the prefix`() {
        assertEquals("2290012345", ok("SN:2290012345"))
        assertEquals("2290012345", ok("S/N: 2290012345"))
        assertEquals("2290012345", ok("Serial Number 2290012345"))
        assertEquals("2290012345", ok("serial=2290012345"))
    }

    @Test
    fun `qr payloads with several fields use the serial field only`() {
        assertEquals("2290012345", ok("SN:2290012345;PN:A77-1234;MAC:00:11:22"))
        assertEquals("2290012345", ok("PN:A77-1234\nSN:2290012345\nHW:2"))
        assertEquals("2290012345", ok("SN:2290012345, PN:A77"))
    }

    @Test
    fun `urls yield the last serial-looking component`() {
        assertEquals("2290012345", ok("https://device.pax.example/register?sn=2290012345"))
        assertEquals("2290012345", ok("https://pax.example/d/2290012345"))
        assertEquals("2290012345", ok("http://x.example/dev/2290012345?src=label"))
    }

    @Test
    fun `garbage, short values and credential codes are rejected`() {
        fails("")
        fails("   ")
        fails("12345")
        fails("hello world!")
        fails("wallee-app-user://v1?u=1&k=abc")
        fails("https://example.com/about")
        fails("a".repeat(33))
    }

    @Test
    fun `failure carries the raw value for the ui`() {
        val e = SerialNumber.parse("??").exceptionOrNull()
        assertTrue(e is SerialNumber.Error.Invalid)
        assertEquals("??", (e as SerialNumber.Error.Invalid).raw)
    }

    @Test
    fun `familiar formats are judged by length only - 10 digits or 8 characters`() {
        // Real labels (2026-09-30): PAX A77 S/N, FEIG cVEND box+ Device-IDs.
        assertTrue(SerialNumber.isFamiliar("1760305860"))
        assertTrue(SerialNumber.isFamiliar("17F91163"))
        assertTrue(SerialNumber.isFamiliar("180FF072"))
        // The other code on the same labels: FEIG "Serial No." and the PAX product code.
        assertFalse(SerialNumber.isFamiliar("7882360"))
        assertFalse(SerialNumber.isFamiliar("A77-2AW-RE6-23EU"))
        assertFalse(SerialNumber.isFamiliar("12345678901"))
        assertFalse(SerialNumber.isFamiliar("17F9-163"))
        // Unfamiliar is a hint, not a rejection: parse still accepts these values.
        assertEquals("7882360", ok("7882360"))
        assertEquals("A77-2AW-RE6-23EU", ok("A77-2AW-RE6-23EU"))
    }

    @Test
    fun `pick prefers the code with a familiar format when a label shows several`() {
        assertEquals("17F91163", SerialNumber.pick(listOf("7882360", "17F91163")))
        assertEquals("S/N:1760305860", SerialNumber.pick(listOf("A77-2AW-RE6-23EU", "S/N:1760305860")))
        assertEquals("7882360", SerialNumber.pick(listOf("7882360")))
        assertEquals("ABC-123456", SerialNumber.pick(listOf("ABC-123456", "??")))
        assertEquals(null, SerialNumber.pick(emptyList()))
    }
}
