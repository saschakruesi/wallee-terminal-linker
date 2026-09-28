package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.core.ui.ControlShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles

enum class WToastKind { Success, Error }

class WToastVisuals(override val message: String, val kind: WToastKind) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/** Shows a wallee toast through a [SnackbarHostState] (the queueing mechanism is Material's, the look is ours). */
suspend fun SnackbarHostState.showToast(message: String, kind: WToastKind = WToastKind.Success) {
    showSnackbar(WToastVisuals(message, kind))
}

/** Host to place at the bottom of a screen; renders [WToast] instead of the dark Material snackbar. */
@Composable
fun WToastHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val kind = (data.visuals as? WToastVisuals)?.kind ?: WToastKind.Success
        WToast(message = data.visuals.message, kind = kind)
    }
}

/** White box with hairline border and a 3 dp bar on the left: turquoise for success, orange for errors. */
@Composable
fun WToast(message: String, kind: WToastKind, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(ControlShape)
                .background(WalleeColors.Bg)
                .border(WalleeSize.Hairline, WalleeColors.Line, ControlShape)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(if (kind == WToastKind.Success) WalleeColors.Turquoise else WalleeColors.Orange),
            )
            Text(
                text = message,
                style = WalleeTextStyles.body,
                modifier = Modifier.padding(horizontal = WalleeSpacing.S2, vertical = 12.dp),
            )
        }
    }
}
