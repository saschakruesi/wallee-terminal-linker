package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wallee.terminallinker.core.ui.WalleeColors

/**
 * Page frame: white background, system-bar and keyboard insets, optional [header] (WHeader) on top and
 * [bottomBar] pinned below the content. A [toastHost] draws wallee toasts above the bottom bar.
 */
@Composable
fun WScreen(
    modifier: Modifier = Modifier,
    background: Color = WalleeColors.Bg,
    applyStatusBarInset: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    toastHost: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .then(if (applyStatusBarInset) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        if (header != null) header()
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) { content() }
            if (toastHost != null) {
                WToastHost(hostState = toastHost, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
        if (bottomBar != null) bottomBar()
    }
}

/** Fixed action bar on white with a hairline on top (terminal detail). */
@Composable
fun WBottomBar(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.background(WalleeColors.Bg)) {
        Hairline()
        content()
    }
}
