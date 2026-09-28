package com.wallee.terminallinker.core.ui

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Material 3 colour scheme fully overridden with wallee tokens (docs/04 §Material-Mapping).
 * `surfaceTint = surface` makes tonal elevation a no-op, all container roles are plain white or
 * turquoise so no Material tonal surfaces or purple accents can appear. There is no dark scheme.
 */
val WalleeColorScheme = lightColorScheme(
    primary = WalleeColors.Black,
    onPrimary = WalleeColors.White,
    primaryContainer = WalleeColors.Turquoise,
    onPrimaryContainer = WalleeColors.Black,
    inversePrimary = WalleeColors.White,
    secondary = WalleeColors.TurquoiseText,
    onSecondary = WalleeColors.White,
    secondaryContainer = WalleeColors.Turquoise20,
    onSecondaryContainer = WalleeColors.Black,
    tertiary = WalleeColors.TurquoiseDeep,
    onTertiary = WalleeColors.White,
    tertiaryContainer = WalleeColors.Turquoise20,
    onTertiaryContainer = WalleeColors.Black,
    background = WalleeColors.Bg,
    onBackground = WalleeColors.Text,
    surface = WalleeColors.Bg,
    onSurface = WalleeColors.Text,
    surfaceVariant = WalleeColors.Bg,
    onSurfaceVariant = WalleeColors.TextMuted,
    surfaceTint = WalleeColors.Bg,
    inverseSurface = WalleeColors.Black,
    inverseOnSurface = WalleeColors.White,
    error = WalleeColors.Orange,
    onError = WalleeColors.White,
    errorContainer = WalleeColors.OrangeSoft,
    onErrorContainer = WalleeColors.OrangeText,
    outline = WalleeColors.Line,
    outlineVariant = WalleeColors.Grid,
    scrim = WalleeColors.Black,
    surfaceBright = WalleeColors.Bg,
    surfaceDim = WalleeColors.Bg,
    surfaceContainer = WalleeColors.Bg,
    surfaceContainerHigh = WalleeColors.Bg,
    surfaceContainerHighest = WalleeColors.Bg,
    surfaceContainerLow = WalleeColors.Bg,
    surfaceContainerLowest = WalleeColors.Bg,
)

private val WalleeSelectionColors = TextSelectionColors(
    handleColor = WalleeColors.Black,
    backgroundColor = WalleeColors.Turquoise40,
)

/** Always light — `isSystemInDarkTheme()` is deliberately ignored (docs/04). */
@Composable
fun WalleeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WalleeColorScheme,
        typography = WalleeTypography,
        shapes = WalleeShapes,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides WalleeColors.Text,
            LocalTextSelectionColors provides WalleeSelectionColors,
        ) {
            ProvideTextStyle(WalleeTextStyles.body, content)
        }
    }
}
