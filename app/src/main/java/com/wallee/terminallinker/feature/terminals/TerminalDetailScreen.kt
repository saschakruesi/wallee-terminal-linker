package com.wallee.terminallinker.feature.terminals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WBadge
import com.wallee.terminallinker.core.ui.components.WBottomBar
import com.wallee.terminallinker.core.ui.components.WConfirmDialog
import com.wallee.terminallinker.core.ui.components.WFactRow
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.core.ui.components.showToast
import com.wallee.terminallinker.di.appViewModel

/** Detail (docs/03 §Terminal-Detail): fact table, inline rename, secondary actions, fixed action bar. */
@Composable
fun TerminalDetailScreen(
    terminalId: Long,
    onBack: () -> Unit,
    onLink: () -> Unit,
    onReplace: () -> Unit,
    onUnlinked: (serial: String?, confirmed: Boolean) -> Unit,
    viewModel: TerminalDetailViewModel = appViewModel { TerminalDetailViewModel(it, terminalId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toasts = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    var confirmUnlink by rememberSaveable { mutableStateOf(false) }
    val none = stringResource(R.string.value_none)
    val terminal = state.terminal
    val language = context.resources.configuration.locales[0]?.language ?: "de"

    LaunchedEffect(Unit) { viewModel.toasts.collect { toasts.showToast(it.message, it.kind) } }
    LaunchedEffect(Unit) { viewModel.unlinked.collect { onUnlinked(it.previousSerial, it.confirmed) } }

    WScreen(
        header = { WHeader(leading = { WBackButton(onClick = onBack) }) },
        toastHost = toasts,
        bottomBar = if (terminal == null || terminal.isDecommissioned) {
            null
        } else {
            {
                ActionBar(terminal, state.busy, viewModel, onLink = onLink, onReplace = {
                    confirmReplace = true
                }, onUnlink = {
                    confirmUnlink =
                        true
                })
            }
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WalleeSpacing.Side),
        ) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            when {
                terminal == null && state.loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spinner()
                        Spacer(Modifier.padding(WalleeSpacing.S1))
                        Text(text = stringResource(R.string.detail_loading), style = WalleeTextStyles.label)
                    }
                }

                terminal == null -> {
                    Text(text = state.error ?: stringResource(R.string.detail_not_found), style = WalleeTextStyles.body)
                    Spacer(Modifier.height(WalleeSpacing.S2))
                    WSecondaryButton(text = stringResource(R.string.action_back), onClick = onBack)
                }

                else -> {
                    if (state.renaming) {
                        RenameRow(state, viewModel)
                    } else {
                        Headline(
                            line1 = terminal.identifier ?: terminal.id.toString(),
                            line2 = terminal.displayName,
                            line2Trailing = {
                                WIconButton(
                                    iconRes = R.drawable.ic_edit,
                                    contentDescription = stringResource(R.string.cd_rename),
                                    onClick = viewModel::startRename,
                                    enabled = !state.busy && !terminal.isDecommissioned,
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(WalleeSpacing.S2))
                    WFactRow(label = stringResource(R.string.detail_label_status)) {
                        WBadge(kind = terminal.badgeKind())
                    }
                    WFactRow(label = stringResource(R.string.detail_label_device), value = terminal.deviceName ?: none)
                    WFactRow(
                        label = stringResource(R.string.detail_label_serial),
                        value =
                            terminal.deviceSerialNumber ?: none,
                    )
                    WFactRow(
                        label = stringResource(R.string.detail_label_type),
                        value =
                            terminal.type?.localizedName(language) ?: none,
                    )
                    WFactRow(
                        label = stringResource(R.string.detail_label_location),
                        value =
                            terminal.locationName ?: none,
                    )
                    WFactRow(
                        label = stringResource(R.string.detail_label_configuration),
                        value =
                            terminal.configurationName ?: none,
                    )
                    WFactRow(
                        label = stringResource(R.string.detail_label_currency),
                        value =
                            terminal.defaultCurrency ?: none,
                    )
                    WFactRow(
                        label = stringResource(R.string.detail_label_activated),
                        value =
                            formatWalleeDate(terminal.activatedOn) ?: none,
                    )
                    terminal.activationCode?.takeIf { it.isNotBlank() }?.let {
                        WFactRow(label = stringResource(R.string.detail_label_activation_code), value = it)
                    }
                    WFactRow(
                        label = stringResource(R.string.detail_label_id),
                        value = terminal.id.toString(),
                        hairline = false,
                    )
                    if (terminal.isDecommissioned) {
                        Spacer(Modifier.height(WalleeSpacing.S2))
                        Text(text = stringResource(R.string.detail_decommissioned), style = WalleeTextStyles.body)
                    }
                    Spacer(Modifier.height(WalleeSpacing.S1))
                    WTextButton(
                        text = stringResource(R.string.detail_action_copy_id),
                        onClick = {
                            clipboard.setText(AnnotatedString(terminal.id.toString()))
                            viewModel.onIdCopied()
                        },
                    )
                    Spacer(Modifier.height(WalleeSpacing.S3))
                }
            }
        }
    }

    if (state.confirmConfiguration) {
        WConfirmDialog(
            title = stringResource(R.string.detail_config_confirm_title),
            text = stringResource(R.string.detail_config_confirm_text),
            confirmText = stringResource(R.string.detail_config_confirm_action),
            onConfirm = viewModel::triggerConfiguration,
            onDismiss = viewModel::dismissTriggerConfiguration,
        )
    }
    if (terminal != null && confirmReplace) {
        WConfirmDialog(
            title = stringResource(R.string.detail_action_replace),
            text = stringResource(
                R.string.detail_confirm_replace,
                terminal.deviceSerialNumber ?: none,
                terminal.displayName,
            ),
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
            text = stringResource(
                R.string.detail_confirm_unlink,
                terminal.deviceSerialNumber ?: none,
                terminal.displayName,
            ),
            confirmText = stringResource(R.string.detail_confirm_unlink_action),
            destructive = true,
            onConfirm = {
                confirmUnlink = false
                viewModel.unlink()
            },
            onDismiss = { confirmUnlink = false },
        )
    }
}

@Composable
private fun RenameRow(state: TerminalDetailUiState, viewModel: TerminalDetailViewModel) {
    Column {
        WInput(
            value = state.renameValue,
            onValueChange = viewModel::onRenameChange,
            label = stringResource(R.string.detail_rename_label),
            enabled = !state.busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.saveRename() }),
        )
        Spacer(Modifier.height(WalleeSpacing.S1))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(WalleeSpacing.S1)) {
            WPrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::saveRename,
                loading = state.busy,
            )
            WSecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = viewModel::cancelRename,
                enabled = !state.busy,
            )
        }
    }
}

@Composable
private fun ActionBar(
    terminal: PaymentTerminal,
    busy: Boolean,
    viewModel: TerminalDetailViewModel,
    onLink: () -> Unit,
    onReplace: () -> Unit,
    onUnlink: () -> Unit,
) {
    WBottomBar {
        Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S2)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WTextButton(
                    text = stringResource(R.string.detail_action_refresh),
                    onClick = viewModel::refresh,
                    enabled = !busy,
                )
                WTextButton(
                    text = stringResource(R.string.detail_action_trigger_config),
                    onClick = viewModel::askTriggerConfiguration,
                    enabled = !busy,
                )
                if (busy) {
                    Spacer(Modifier.padding(WalleeSpacing.S1))
                    Spinner(size = 20.dp())
                }
            }
            Spacer(Modifier.height(WalleeSpacing.S1))
            if (terminal.linked) {
                WPrimaryButton(
                    text = stringResource(R.string.detail_action_replace),
                    onClick = onReplace,
                    large = true,
                    enabled = !busy,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                WTextButton(
                    text = stringResource(R.string.detail_action_unlink),
                    onClick = onUnlink,
                    destructive = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                WPrimaryButton(
                    text = stringResource(R.string.detail_action_link),
                    onClick = onLink,
                    large = true,
                    enabled = !busy,
                )
            }
        }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())

@Suppress("unused")
private val unusedColorRef = WalleeColors.Bg
