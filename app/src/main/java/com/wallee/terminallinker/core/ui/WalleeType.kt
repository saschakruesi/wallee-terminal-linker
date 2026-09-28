package com.wallee.terminallinker.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.wallee.terminallinker.R

/** Roboto 300/400/500/700 from res/font — no downloadable-fonts provider. */
val Roboto = FontFamily(
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold),
)

private val NoTrim = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)

private fun roboto(
    weight: FontWeight,
    size: Int,
    lineHeightFactor: Float = 1.5f,
    color: androidx.compose.ui.graphics.Color,
    fontFeatureSettings: String? = null,
): TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeightFactor).sp,
    letterSpacing = 0.sp,
    color = color,
    fontFeatureSettings = fontFeatureSettings,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = NoTrim,
)

/** Text roles from docs/04 §Typografie. Always left-aligned, no italics, no uppercase, no letter spacing. */
object WalleeTextStyles {
    /** Headline line 1: Light 24 sp, muted. */
    val headline1 = roboto(FontWeight.Light, 24, 1.25f, WalleeColors.TextMuted)

    /** Headline line 2: Medium 24 sp, black. */
    val headline2 = roboto(FontWeight.Medium, 24, 1.25f, WalleeColors.Black)

    /** Section title: Medium 17 sp, black. */
    val sectionTitle = roboto(FontWeight.Medium, 17, 1.4f, WalleeColors.Black)

    /** Body, forms: Regular 15 sp, #333, line height 1.5. */
    val body = roboto(FontWeight.Normal, 15, 1.5f, WalleeColors.Text)

    /** List row title: Medium 15 sp, black. */
    val bodyTitle = roboto(FontWeight.Medium, 15, 1.4f, WalleeColors.Black)

    /** Labels, list row line 2, table header: Regular 13 sp, muted. */
    val label = roboto(FontWeight.Normal, 13, 1.4f, WalleeColors.TextMuted)

    /** Footnotes, status line: Light 12 sp, muted. */
    val footnote = roboto(FontWeight.Light, 12, 1.4f, WalleeColors.TextMuted)

    /** Serial number in the confirmation sheet: Medium 32 sp, black, tabular figures. */
    val serial = roboto(FontWeight.Medium, 32, 1.2f, WalleeColors.Black, fontFeatureSettings = "tnum")

    /** Statement on a turquoise panel: Regular 24 sp, black. */
    val statement = roboto(FontWeight.Normal, 24, 1.3f, WalleeColors.Black)

    /** Button label: Medium 15 sp, no letter spacing, no uppercase. */
    val button = roboto(FontWeight.Medium, 15, 1.2f, WalleeColors.Black)

    /** Badge label: Medium 12 sp. */
    val badge = roboto(FontWeight.Medium, 12, 1.2f, WalleeColors.Text)

    /** Tabular values in fact tables. */
    val value = roboto(FontWeight.Normal, 15, 1.4f, WalleeColors.Text, fontFeatureSettings = "tnum")

    /** Display numbers ≥ 40 sp (only use of Roboto Bold). */
    val display = roboto(FontWeight.Bold, 40, 1.1f, WalleeColors.Black, fontFeatureSettings = "tnum")
}

/** Material typography mapped onto wallee roles so no default Material text style leaks through. */
val WalleeTypography = Typography(
    displayLarge = WalleeTextStyles.display,
    displayMedium = WalleeTextStyles.serial,
    displaySmall = WalleeTextStyles.statement,
    headlineLarge = WalleeTextStyles.headline2,
    headlineMedium = WalleeTextStyles.headline2,
    headlineSmall = WalleeTextStyles.headline1,
    titleLarge = WalleeTextStyles.sectionTitle,
    titleMedium = WalleeTextStyles.bodyTitle,
    titleSmall = WalleeTextStyles.bodyTitle,
    bodyLarge = WalleeTextStyles.body,
    bodyMedium = WalleeTextStyles.body,
    bodySmall = WalleeTextStyles.label,
    labelLarge = WalleeTextStyles.button,
    labelMedium = WalleeTextStyles.label,
    labelSmall = WalleeTextStyles.footnote,
)
