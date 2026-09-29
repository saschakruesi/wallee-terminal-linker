package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.core.ui.SheetShape
import com.wallee.terminallinker.core.ui.WalleeColors

/** White sheet, 8 dp top radius, 32×4 dp grey handle, 40 % black scrim without blur, no tonal tint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The experimental SheetState stays inside this wrapper so callers need no opt-in.
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = WalleeColors.Bg,
        contentColor = WalleeColors.Text,
        scrimColor = WalleeColors.Scrim,
        dragHandle = { SheetHandle() },
        content = content,
    )
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .width(32.dp)
            .height(4.dp)
            .background(WalleeColors.Line, RoundedCornerShape(2.dp)),
    )
}
