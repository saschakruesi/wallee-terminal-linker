package com.wallee.terminallinker.core.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppVersionTest {
    @Test
    fun `parses tags and version names`() {
        assertEquals(listOf(1, 2, 0), AppVersion.parse("v1.2.0"))
        assertEquals(listOf(1, 1, 0), AppVersion.parse("1.1.0-debug"))
        assertEquals(listOf(0, 9), AppVersion.parse("0.9"))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse(""))
    }

    @Test
    fun `compares numerically and ignores suffixes`() {
        assertTrue(AppVersion.isNewer("v1.2.0", "1.1.9-debug"))
        assertTrue(AppVersion.isNewer("v1.0.0", "0.9"))
        assertTrue(AppVersion.isNewer("1.0.10", "1.0.9"))
        assertFalse(AppVersion.isNewer("v1.0.0", "1.0.0"))
        assertFalse(AppVersion.isNewer("v1.0", "1.0.0"))
        assertFalse(AppVersion.isNewer("v0.9.9", "1.0.0"))
        assertFalse(AppVersion.isNewer("nightly", "1.0.0"))
        assertFalse(AppVersion.isNewer("v2.0.0", "unknown"))
    }
}
