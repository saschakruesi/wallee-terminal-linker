package com.wallee.terminallinker.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii: 4 dp inputs/buttons, 8 dp cards/sheets, 12 dp badges. Nothing pill-shaped except badges. */
object WalleeRadius {
    val Control = 4.dp
    val Card = 8.dp
    val Badge = 12.dp
}

val ControlShape = RoundedCornerShape(WalleeRadius.Control)
val CardShape = RoundedCornerShape(WalleeRadius.Card)
val SheetShape = RoundedCornerShape(topStart = WalleeRadius.Card, topEnd = WalleeRadius.Card)
val BadgeShape = RoundedCornerShape(WalleeRadius.Badge)

val WalleeShapes = Shapes(
    extraSmall = ControlShape,
    small = ControlShape,
    medium = CardShape,
    large = CardShape,
    extraLarge = CardShape,
)
