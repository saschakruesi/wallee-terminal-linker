package com.wallee.terminallinker.core.ui

import androidx.compose.ui.graphics.Color

/**
 * wallee colour tokens (docs/04-design-system.md). Turquoise is a surface colour, never text on white;
 * the only turquoise-looking text colour on white is [WalleeColors.TurquoiseText].
 */
object WalleeColors {
    val Turquoise = Color(0xFF11D9CC)
    val Turquoise80 = Color(0xFF41E1D6)
    val Turquoise40 = Color(0xFF9CEDE7)
    val Turquoise20 = Color(0xFFCFF7F4)
    val TurquoiseText = Color(0xFF0B8F87)
    val TurquoiseDeep = Color(0xFF0E6B66)

    val Orange = Color(0xFFFF4D00)
    val OrangeSoft = Color(0xFFFFE4D9)
    val OrangeText = Color(0xFFB33600)

    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Text = Color(0xFF333333)
    val TextMuted = Color(0xFF808080)
    val Line = Color(0xFFD9D9D9)
    val Grid = Color(0xFFE6E6E6)
    val Bg = Color(0xFFFFFFFF)
    val BgSoft = Color(0xFFF7F7F7)
    val BadgeNeutral = Color(0xFFF0F0F0)

    /** Pressed state of the black primary button. */
    val PrimaryPressed = Color(0xFF333333)

    /** Scrim behind bottom sheets: black 40 %, no blur. */
    val Scrim = Color(0x66000000)

    /** Scanner overlay dimming: black 60 %. */
    val ScannerDim = Color(0x99000000)
}
