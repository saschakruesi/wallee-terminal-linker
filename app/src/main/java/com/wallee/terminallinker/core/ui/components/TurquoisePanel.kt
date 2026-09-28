package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing

/**
 * Flat `#11D9CC` surface with 40 dp inner padding and black content. The white wordmark (36 dp) is shown
 * only when the panel dominates the screen (setup, result).
 */
@Composable
fun TurquoisePanel(
    modifier: Modifier = Modifier,
    showLogo: Boolean = false,
    contentPadding: Dp = WalleeSpacing.S5,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WalleeColors.Turquoise)
            .padding(contentPadding),
    ) {
        if (showLogo) {
            WalleeLogo(variant = LogoVariant.White, height = WalleeSize.LogoPanel)
            Spacer(Modifier.height(WalleeSpacing.S3))
        }
        CompositionLocalProvider(LocalContentColor provides WalleeColors.Black) {
            content()
        }
    }
}
