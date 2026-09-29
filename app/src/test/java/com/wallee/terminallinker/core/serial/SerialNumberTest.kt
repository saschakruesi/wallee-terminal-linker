package com.wallee.terminallinker.core.serial

import org.junit.jupiter.api.Assertions.assertEquals
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
}
