package com.wallee.terminallinker.feature.terminals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WBadge
import com.wallee.terminallinker.core.ui.components.WBottomBar
import com.wallee.terminallinker.core.ui.components.WConfirmDialog
import com.wallee.terminallinker.core.ui.components.WFactRow
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.core.ui.components.showToast
import kotlinx.coroutines.launch

/** Detail layout from docs/03 with the fact table and the fixed action bar; data is phase-1 sample data. */
@Composable
fun TerminalDetailScreen(
    terminalId: Long,
    onBack: () -> Unit,
    onLink: () -> Unit,
    onReplace: () -> Unit,
    onUnlinked: (serial: String?) -> Unit,
) {
    val terminal = SampleTerminals.byId(terminalId)
    val toasts = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    var confirmUnlink by rememberSaveable { mutableStateOf(false) }
    val none = stringResource(R.string.value_none)
    val refreshedText = stringResource(R.string.toast_placeholder_success)
    val decommissioned = terminal?.state == "DECOMMISSIONING" || terminal?.state == "DECOMMISSIONED"

    WScreen(
        header = { WHeader(leading = { WBackButton(onClick = onBack) }) },
        toastHost = toasts,
        bottomBar = if (terminal == null || decommissioned) {
            null
        } else {
            {
                WBottomBar {
                    Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S2)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            WTextButton(
                                text = stringResource(R.string.detail_action_refresh),
                                onClick = {
                                    scope.launch { toasts.showToast(refreshedText) }
                                },
                            )
                            WTextButton(text = stringResource(R.string.detail_action_trigger_config), onClick = {})
                        }
                        Spacer(Modifier.height(WalleeSpacing.S1))
                        if (terminal.linked) {
                            WPrimaryButton(
                                text = stringResource(R.string.detail_action_replace),
                                onClick = { confirmReplace = true },
                                large = true,
                            )
                            Spacer(Modifier.height(WalleeSpacing.S1))
                            WTextButton(
                                text = stringResource(R.string.detail_action_unlink),
                                onClick = { confirmUnlink = true },
                                destructive = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            WPrimaryButton(
                                text = stringResource(R.string.detail_action_link),
                                onClick = onLink,
                                large = true,
                            )
                        }
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WalleeSpacing.Side),
        ) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            if (terminal == null) {
                Text(text = stringResource(R.string.terminals_empty), style = WalleeTextStyles.body)
                return@Column
            }
            Headline(
                line1 = terminal.identifier,
                line2 = terminal.name,
                line2Trailing = {
                    WIconButton(
                        iconRes = R.drawable.ic_edit,
                        contentDescription = stringResource(R.string.cd_rename),
                        onClick = {},
                    )
                },
            )
            Spacer(Modifier.height(WalleeSpacing.S2))
            WFactRow(label = stringResource(R.string.detail_label_status)) { WBadge(kind = terminal.badgeKind()) }
            WFactRow(label = stringResource(R.string.detail_label_device), value = terminal.deviceName ?: none)
            WFactRow(label = stringResource(R.string.detail_label_serial), value = terminal.deviceSerialNumber ?: none)
            WFactRow(label = stringResource(R.string.detail_label_type), value = terminal.type)
            WFactRow(label = stringResource(R.string.detail_label_location), value = terminal.location)
            WFactRow(label = stringResource(R.string.detail_label_configuration), value = terminal.configuration)
            WFactRow(label = stringResource(R.string.detail_label_currency), value = terminal.currency)
            WFactRow(label = stringResource(R.string.detail_label_activated), value = terminal.activatedOn ?: none)
            WFactRow(label = stringResource(R.string.detail_label_id), value = terminal.id.toString(), hairline = false)
            if (decommissioned) {
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(text = stringResource(R.string.detail_decommissioned), style = WalleeTextStyles.body)
            }
            Spacer(Modifier.height(WalleeSpacing.S3))
        }
    }

    if (terminal != null && confirmReplace) {
        WConfirmDialog(
            title = stringResource(R.string.detail_action_replace),
            text = stringResource(R.string.detail_confirm_replace, terminal.deviceSerialNumber ?: none, terminal.name),
            confirmText = stringResource(R.string.detail_confirm_replace_action),
            onConfirm = {
                confirmReplace = false
                onReplace()
            },
            onDismiss = { confirmReplace = false },
        )
    }
    if (terminal != null && confirmUnlink) {
        WConfirmDialog(
            title = stringResource(R.string.detail_action_unlink),
            text = stringResource(R.string.detail_confirm_unlink, terminal.deviceSerialNumber ?: none, terminal.name),
            confirmText = stringResource(R.string.detail_confirm_unlink_action),
            destructive = true,
            onConfirm = {
                confirmUnlink = false
                onUnlinked(terminal.deviceSerialNumber)
            },
            onDismiss = { confirmUnlink = false },
        )
    }
}
