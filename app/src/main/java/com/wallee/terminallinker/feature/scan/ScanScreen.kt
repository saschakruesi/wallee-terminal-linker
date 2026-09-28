package com.wallee.terminallinker.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.ViewfinderCorners
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.feature.terminals.SampleTerminals
import com.wallee.terminallinker.navigation.ScanMode

/**
 * Scanner layout from docs/03 without a camera (phase 4). Black background, viewfinder corners, hint,
 * manual entry sheet and a debug "simulate hit" action so the confirmation sheet can be reviewed.
 */
@Composable
fun ScanScreen(
    mode: ScanMode,
    terminalId: Long?,
    onClose: () -> Unit,
    onLinked: (terminalId: Long, serial: String, previousSerial: String?) -> Unit,
    onCredentialsScanned: () -> Unit,
    spacesOnly: Boolean = false,
) {
    val terminal = terminalId?.let(SampleTerminals::byId)
    var manualOpen by rememberSaveable { mutableStateOf(false) }
    var confirmSerial by rememberSaveable { mutableStateOf<String?>(null) }
    var manualValue by rememberSaveable { mutableStateOf("") }
    val credentials = mode == ScanMode.CREDENTIALS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WalleeColors.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WalleeSpacing.S1, vertical = WalleeSpacing.S1),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onClose,
                tint = WalleeColors.White,
            )
            WIconButton(
                iconRes = R.drawable.ic_flashlight,
                contentDescription = stringResource(R.string.cd_torch),
                onClick = {},
                tint = WalleeColors.White,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WalleeSpacing.Side),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(96.dp))
            if (credentials) {
                ViewfinderCorners(width = 240.dp, height = 240.dp)
            } else {
                ViewfinderCorners(width = 280.dp, height = 160.dp)
            }
            Spacer(Modifier.height(WalleeSpacing.S4))
            Text(
                text = stringResource(if (credentials) R.string.scan_hint_credentials else R.string.scan_hint_serial),
                style = WalleeTextStyles.body,
                color = WalleeColors.White,
                textAlign = TextAlign.Center,
            )
            if (terminal != null) {
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(
                    text = stringResource(R.string.scan_terminal_line, terminal.name, terminal.identifier),
                    style = WalleeTextStyles.label,
                    color = WalleeColors.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            if (!credentials) {
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(
                    text = stringResource(R.string.scan_placeholder_note),
                    style = WalleeTextStyles.footnote,
                    color = WalleeColors.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!credentials) {
                WSecondaryButton(
                    text = stringResource(R.string.scan_manual_entry),
                    onClick = { manualOpen = true },
                    onDark = true,
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                WTextButton(
                    text = stringResource(R.string.scan_simulate_hit),
                    onClick = { confirmSerial = "2290098765" },
                    onDark = true,
                )
            } else {
                CredentialScanControls(spacesOnly = spacesOnly, onApplied = onCredentialsScanned)
            }
            Spacer(Modifier.height(WalleeSpacing.S3))
        }
    }

    if (manualOpen) {
        WBottomSheet(onDismissRequest = { manualOpen = false }) {
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
                Text(text = stringResource(R.string.scan_manual_entry), style = WalleeTextStyles.sectionTitle)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = manualValue,
                    onValueChange = { manualValue = it.uppercase() },
                    label = stringResource(R.string.detail_label_serial),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
                WPrimaryButton(
                    text = stringResource(R.string.action_continue),
                    onClick = {
                        manualOpen = false
                        confirmSerial = manualValue.trim()
                    },
                    enabled = manualValue.trim().length >= 6,
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }

    val serial = confirmSerial
    if (serial != null && terminal != null) {
        WBottomSheet(onDismissRequest = { confirmSerial = null }) {
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
                Text(text = stringResource(R.string.scan_confirm_title), style = WalleeTextStyles.sectionTitle)
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(text = serial, style = WalleeTextStyles.serial)
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(
                    text = stringResource(R.string.scan_confirm_link_with, terminal.name, terminal.identifier),
                    style = WalleeTextStyles.body,
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
                WPrimaryButton(
                    text = stringResource(
                        if (mode ==
                            ScanMode.REPLACE
                        ) {
                            R.string.scan_confirm_replace
                        } else {
                            R.string.scan_confirm_link
                        },
                    ),
                    onClick = { onLinked(terminal.id, serial, terminal.deviceSerialNumber) },
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                WSecondaryButton(
                    text = stringResource(R.string.scan_rescan),
                    onClick = { confirmSerial = null },
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }
}
