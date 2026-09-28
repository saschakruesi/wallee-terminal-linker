package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors

/** 2 dp stroke, no track. `TurquoiseText` on white, black on turquoise; [color] overrides both. */
@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 24.dp, onTurquoise: Boolean = false, color: Color? = null) {
    val description = stringResourceCompat(R.string.cd_loading)
    CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        color = color ?: if (onTurquoise) WalleeColors.Black else WalleeColors.TurquoiseText,
        strokeWidth = 2.dp,
        trackColor = Color.Transparent,
    )
}
