package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.core.ui.WalleeColors

/** Viewfinder corners: 3 dp turquoise, 24 dp long. The surrounding dimming is drawn by the scanner screen. */
@Composable
fun ViewfinderCorners(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerLength: Dp = 24.dp,
    strokeWidth: Dp = 3.dp,
) {
    Canvas(modifier = modifier.size(width, height)) {
        val stroke = strokeWidth.toPx()
        val len = cornerLength.toPx()
        val half = stroke / 2f
        val w = size.width
        val h = size.height
        val color = WalleeColors.Turquoise
        fun line(from: Offset, to: Offset) = drawLine(color, from, to, strokeWidth = stroke, cap = StrokeCap.Square)
        // top-left
        line(Offset(half, half), Offset(len, half))
        line(Offset(half, half), Offset(half, len))
        // top-right
        line(Offset(w - half, half), Offset(w - len, half))
        line(Offset(w - half, half), Offset(w - half, len))
        // bottom-left
        line(Offset(half, h - half), Offset(len, h - half))
        line(Offset(half, h - half), Offset(half, h - len))
        // bottom-right
        line(Offset(w - half, h - half), Offset(w - len, h - half))
        line(Offset(w - half, h - half), Offset(w - half, h - len))
    }
}
