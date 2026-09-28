package com.wallee.terminallinker.core.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the contrast rules from docs/04 §Farben with the real WCAG ratios of the tokens.
 * Note: TurquoiseText on white is 3.97:1 and TextMuted on white 3.95:1 — both pass the 3:1 level for
 * large text and UI components but not 4.5:1 for body text; the tokens are prescribed by the design system.
 */
class WalleeColorsTest {
    @Test
    fun `turquoise text is the only turquoise usable as text on white`() {
        assertTrue(Contrast.ratio(WalleeColors.TurquoiseText, WalleeColors.Bg) >= 3.0)
        assertTrue(Contrast.ratio(WalleeColors.Turquoise, WalleeColors.Bg) < 3.0)
    }

    @Test
    fun `black and body text pass AA on the turquoise panel`() {
        assertTrue(Contrast.ratio(WalleeColors.Black, WalleeColors.Turquoise) >= 4.5)
        assertTrue(Contrast.ratio(WalleeColors.Text, WalleeColors.Turquoise) >= 4.5)
    }

    @Test
    fun `badge and muted text colours reach at least the large-text level`() {
        assertTrue(Contrast.ratio(WalleeColors.TurquoiseText, WalleeColors.Turquoise20) >= 3.0)
        assertTrue(Contrast.ratio(WalleeColors.OrangeText, WalleeColors.OrangeSoft) >= 4.5)
        assertTrue(Contrast.ratio(WalleeColors.Text, WalleeColors.BadgeNeutral) >= 4.5)
        assertTrue(Contrast.ratio(WalleeColors.TextMuted, WalleeColors.Bg) >= 3.0)
    }

    @Test
    fun `orange is a marker colour, not body text`() {
        // 3.33:1 on white — fine for 13 sp error markers and icons, deliberately never used for body text.
        assertTrue(Contrast.ratio(WalleeColors.Orange, WalleeColors.Bg) >= 3.0)
        assertTrue(Contrast.ratio(WalleeColors.Orange, WalleeColors.Bg) < 4.5)
    }

    @Test
    fun `material colour scheme carries no default purple`() {
        val scheme = WalleeColorScheme
        assertEquals(WalleeColors.Black, scheme.primary)
        assertEquals(WalleeColors.Turquoise, scheme.primaryContainer)
        assertEquals(WalleeColors.Bg, scheme.surface)
        assertEquals(WalleeColors.Bg, scheme.surfaceTint)
        assertEquals(WalleeColors.Orange, scheme.error)
        assertEquals(WalleeColors.Line, scheme.outline)
    }

    @Test
    fun `contrast ratio is symmetric and white on black is 21`() {
        assertEquals(21.0, Contrast.ratio(WalleeColors.White, WalleeColors.Black), 0.01)
        assertEquals(
            Contrast.ratio(WalleeColors.Text, WalleeColors.Bg),
            Contrast.ratio(WalleeColors.Bg, WalleeColors.Text),
            1e-9,
        )
    }
}
