package com.wallee.terminallinker.feature.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.CardShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.TurquoisePanel
import com.wallee.terminallinker.core.ui.components.WCheckbox
import com.wallee.terminallinker.core.ui.components.WConfirmDialog
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.di.appViewModel

/**
 * Setup (docs/03): full-turquoise top with the wordmark, then the credentials form with connection test.
 * Way A = typed credentials, way B = QR code (scanner in phase 4, paste placeholder until then).
 */
@Composable
fun SetupScreen(
    onContinue: () -> Unit,
    onScanCredentials: () -> Unit,
    viewModel: SetupViewModel = appViewModel { SetupViewModel(it) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.consumeHandoff()
        viewModel.done.collect { onContinue() }
    }

    WScreen(applyStatusBarInset = false) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            TurquoisePanel(showLogo = true, behindStatusBar = true) {
                Text(
                    text = stringResource(R.string.brand_claim),
                    style = WalleeTextStyles.statement.copy(fontWeight = FontWeight.Light),
                    color = WalleeColors.Black,
                )
            }
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side)) {
                Spacer(Modifier.height(WalleeSpacing.S4))
                Headline(
                    line1 = stringResource(R.string.setup_title_1),
                    line2 =
                        state.label ?: stringResource(R.string.setup_title_2),
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.setup_section_credentials),
                        style = WalleeTextStyles.sectionTitle,
                    )
                    WSecondaryButton(
                        text = stringResource(R.string.setup_qr_button),
                        iconRes = R.drawable.ic_qr_scan,
                        onClick = onScanCredentials,
                        enabled = !state.testing,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(text = stringResource(R.string.setup_qr_hint), style = WalleeTextStyles.footnote)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = state.userId,
                    onValueChange = viewModel::onUserIdChange,
                    label = stringResource(R.string.setup_user_id),
                    enabled = !state.testing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                if (state.keyStored) {
                    Text(text = stringResource(R.string.setup_auth_key), style = WalleeTextStyles.label)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.setup_key_masked), style = WalleeTextStyles.body)
                        Spacer(Modifier.width(WalleeSpacing.S1))
                        WTextButton(
                            text = stringResource(R.string.setup_key_change),
                            onClick = viewModel::onEditStoredKey,
                            enabled = !state.testing,
                        )
                    }
                    Text(text = stringResource(R.string.setup_key_stored_hint), style = WalleeTextStyles.footnote)
                } else {
                    WInput(
                        value = state.key,
                        onValueChange = viewModel::onKeyChange,
                        label = stringResource(R.string.setup_auth_key),
                        password = true,
                        enabled = !state.testing,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (state.canSubmit) viewModel.testAndSave()
                            },
                        ),
                    )
                }
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(text = stringResource(R.string.setup_help), style = WalleeTextStyles.label)
                WTextButton(text = stringResource(R.string.setup_help_link), onClick = {})
                Spacer(Modifier.height(WalleeSpacing.S1))
                WCheckbox(
                    checked = state.remember,
                    onCheckedChange = viewModel::onRememberChange,
                    label = stringResource(R.string.setup_remember),
                    enabled = !state.testing,
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                WPrimaryButton(
                    text = stringResource(R.string.setup_test_and_save),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.testAndSave()
                    },
                    enabled = state.canSubmit || state.testing,
                    loading = state.testing,
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    state.error?.let { Text(text = it, style = WalleeTextStyles.label, color = WalleeColors.Orange) }
                    state.foundCount?.let {
                        Text(
                            text = pluralStringResource(R.plurals.setup_spaces_found, it, it),
                            style = WalleeTextStyles.label,
                            color = WalleeColors.TurquoiseText,
                        )
                    }
                }
                if (state.manualPanel) {
                    Spacer(Modifier.height(WalleeSpacing.S2))
                    ManualSpacePanel(state, viewModel)
                }
                Spacer(Modifier.height(WalleeSpacing.S2))
                WTextButton(
                    text = stringResource(R.string.setup_credentials_from_qr),
                    onClick = onScanCredentials,
                    enabled = !state.testing,
                )
                Spacer(Modifier.height(WalleeSpacing.S4))
            }
        }
    }

    state.replaceDialog?.let { scanned ->
        WConfirmDialog(
            title = stringResource(R.string.setup_replace_title),
            text = stringResource(R.string.setup_replace_text, scanned.userId.toString()),
            confirmText = stringResource(R.string.setup_replace_confirm),
            onConfirm = viewModel::onReplaceConfirmed,
            onDismiss = viewModel::onReplaceDismissed,
        )
    }
}

/** Inline panel "Keine Spaces gefunden" with manual space ID entry (docs/03 §Setup, docs/02 §3.1 step 2). */
@Composable
private fun ManualSpacePanel(state: SetupUiState, viewModel: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WalleeColors.Turquoise20, CardShape)
            .padding(WalleeSpacing.S2),
    ) {
        Text(text = stringResource(R.string.setup_no_spaces_title), style = WalleeTextStyles.sectionTitle)
        Spacer(Modifier.height(WalleeSpacing.S1))
        Text(
            text = stringResource(
                if (state.manualForbidden) R.string.setup_no_spaces_forbidden else R.string.setup_no_spaces_text,
            ),
            style = WalleeTextStyles.body,
        )
        Spacer(Modifier.height(WalleeSpacing.S2))
        Row(verticalAlignment = Alignment.Bottom) {
            WInput(
                value = state.manualId,
                onValueChange = viewModel::onManualIdChange,
                label = stringResource(R.string.setup_manual_space_id),
                enabled = !state.manualChecking,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.verifyManual() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(WalleeSpacing.S1))
            if (state.manualChecking) {
                Spinner(modifier = Modifier.padding(12.dp))
            } else {
                WSecondaryButton(
                    text = stringResource(R.string.setup_check),
                    onClick = viewModel::verifyManual,
                    enabled = state.manualId.isNotBlank(),
                )
            }
        }
        state.manualError?.let {
            Spacer(Modifier.height(4.dp))
            Text(text = it, style = WalleeTextStyles.label, color = WalleeColors.Orange)
        }
        state.manualVerified?.let { space ->
            Spacer(Modifier.height(WalleeSpacing.S2))
            Text(
                text = stringResource(R.string.setup_verified_space, space.name, space.id),
                style = WalleeTextStyles.bodyTitle,
            )
            Spacer(Modifier.height(WalleeSpacing.S1))
            WPrimaryButton(
                text = stringResource(R.string.setup_add),
                onClick = viewModel::addManualAndContinue,
                large = true,
            )
        }
    }
}
