package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.CardShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles

/** White dialog, 8 dp radius, title 17 sp Medium, body 15 sp, buttons right-aligned. No elevation. */
@Composable
fun WDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = CardShape,
            color = WalleeColors.Bg,
            contentColor = WalleeColors.Text,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(WalleeSpacing.S3)) {
                Text(text = title, style = WalleeTextStyles.sectionTitle)
                Spacer(Modifier.height(12.dp))
                Text(text = text, style = WalleeTextStyles.body)
                if (content != null) {
                    Spacer(Modifier.height(WalleeSpacing.S2))
                    content()
                }
                Spacer(Modifier.height(WalleeSpacing.S3))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (dismissButton != null) {
                        Box(modifier = Modifier.weight(1f, fill = false)) { dismissButton() }
                        Spacer(Modifier.width(WalleeSpacing.S1))
                    }
                    Box(modifier = Modifier.weight(1f, fill = false)) { confirmButton() }
                }
            }
        }
    }
}

/**
 * Confirmation dialog for actions with effect on wallee. Destructive actions (unlink) use an orange text
 * button, everything else the black primary button; cancel is always a secondary button.
 */
@Composable
fun WConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = stringResource(R.string.action_cancel),
) {
    WDialog(
        title = title,
        text = text,
        onDismissRequest = onDismiss,
        dismissButton = { WSecondaryButton(text = dismissText, onClick = onDismiss) },
        confirmButton = {
            if (destructive) {
                WTextButton(text = confirmText, onClick = onConfirm, destructive = true)
            } else {
                WPrimaryButton(text = confirmText, onClick = onConfirm)
            }
        },
    )
}
