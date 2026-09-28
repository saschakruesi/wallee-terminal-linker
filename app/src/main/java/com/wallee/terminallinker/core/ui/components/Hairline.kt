package com.wallee.terminallinker.core.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize

/** 1 dp `#D9D9D9` separator — the only way rows and sections are divided (no shadows, no card borders). */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = WalleeSize.Hairline, color = WalleeColors.Line)
}
