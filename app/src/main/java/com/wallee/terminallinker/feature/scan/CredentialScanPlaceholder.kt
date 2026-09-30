package com.wallee.terminallinker.feature.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.api.toUserMessage
import com.wallee.terminallinker.core.auth.CredentialQr
import com.wallee.terminallinker.core.auth.ScannedCredentials
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WFactRow
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.di.appContainer

/**
 * Credential-QR handling until the camera scanner arrives in phase 4: a debug-only paste sheet feeds
 * [CredentialQr.parse]; the result goes through the confirmation sheet from docs/03 (key never shown)
 * and is handed over in memory via [com.wallee.terminallinker.core.auth.CredentialHandoff].
 */
@Composable
fun CredentialScanControls(
    spacesOnly: Boolean,
    onApplied: () -> Unit,
    externalRaw: String? = null,
    onExternalConsumed: () -> Unit = {},
) {
    val container = appContainer()
    val context = LocalContext.current
    var pasteOpen by remember { mutableStateOf(false) }
    var raw by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<ScannedCredentials?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(externalRaw) {
        val value = externalRaw ?: return@LaunchedEffect
        CredentialQr.parse(value).fold(
            onSuccess = { parsed = it },
            onFailure = { error = it.toUserMessage(context) },
        )
        onExternalConsumed()
    }

    if (BuildConfig.DEBUG) {
        WSecondaryButton(text = stringResource(R.string.qr_paste_label), onClick = {
            pasteOpen = true
        }, onDark = true, large = true)
        Spacer(Modifier.height(WalleeSpacing.S1))
    }

    if (pasteOpen) {
        WBottomSheet(onDismissRequest = { pasteOpen = false }) {
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
                WInput(
                    value = raw,
                    onValueChange = { raw = it },
                    label = stringResource(R.string.qr_paste_label),
                    placeholder = stringResource(R.string.qr_paste_hint),
                    password = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                WPrimaryButton(
                    text = stringResource(R.string.qr_parse),
                    onClick = {
                        pasteOpen = false
                        CredentialQr.parse(raw).fold(
                            onSuccess = { parsed = it },
                            onFailure = { error = it.toUserMessage(context) },
                        )
                        raw = ""
                    },
                    enabled = raw.isNotBlank(),
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }

    error?.let { message ->
        WBottomSheet(onDismissRequest = { error = null }) {
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
                Text(text = stringResource(R.string.qr_not_wallee_title), style = WalleeTextStyles.sectionTitle)
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(text = message, style = WalleeTextStyles.body)
                Spacer(Modifier.height(WalleeSpacing.S3))
                WPrimaryButton(text = stringResource(R.string.qr_rescan), onClick = { error = null }, large = true)
                Spacer(Modifier.height(WalleeSpacing.S1))
                WTextButton(text = stringResource(R.string.action_cancel), onClick = { error = null })
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }

    parsed?.let { creds ->
        WBottomSheet(onDismissRequest = { parsed = null }) {
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
                Text(
                    text = stringResource(
                        if (spacesOnly) R.string.qr_detected_spaces_title else R.string.qr_detected_title,
                    ),
                    style = WalleeTextStyles.sectionTitle,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                if (!spacesOnly) {
                    WFactRow(label = stringResource(R.string.qr_user_id), value = creds.userId.toString())
                    WFactRow(label = stringResource(R.string.qr_key), value = stringResource(R.string.setup_key_masked))
                }
                WFactRow(
                    label = stringResource(R.string.qr_spaces),
                    value =
                    creds.spaceIds.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: stringResource(R.string.value_none),
                )
                WFactRow(
                    label = stringResource(R.string.qr_label),
                    value =
                    creds.label ?: stringResource(R.string.value_none),
                    hairline = false,
                )
                if (spacesOnly && creds.spaceIds.isEmpty()) {
                    Spacer(Modifier.height(WalleeSpacing.S1))
                    Text(
                        text = stringResource(R.string.qr_no_spaces_in_code),
                        style = WalleeTextStyles.label,
                        color = WalleeColors.Orange,
                    )
                }
                Spacer(Modifier.height(WalleeSpacing.S3))
                WPrimaryButton(
                    text = stringResource(if (spacesOnly) R.string.qr_apply_spaces else R.string.qr_apply),
                    onClick = {
                        container.credentialHandoff.offer(if (spacesOnly) creds.copy(key = "") else creds)
                        parsed = null
                        onApplied()
                    },
                    enabled = !spacesOnly || creds.spaceIds.isNotEmpty(),
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                WTextButton(text = stringResource(R.string.action_cancel), onClick = { parsed = null })
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }
}
