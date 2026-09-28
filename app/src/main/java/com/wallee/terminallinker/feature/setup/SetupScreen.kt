package com.wallee.terminallinker.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.TurquoisePanel
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WTextButton

/**
 * Setup layout from docs/03: full-turquoise top third with the white wordmark and claim, then the
 * credentials form. Phase 1 placeholder: the form is visual only; the connection test comes in phase 2.
 */
@Composable
fun SetupScreen(onContinue: () -> Unit, onScanCredentials: () -> Unit) {
    var userId by rememberSaveable { mutableStateOf("") }
    var authKey by rememberSaveable { mutableStateOf("") }
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
                Headline(line1 = stringResource(R.string.setup_title_1), line2 = stringResource(R.string.setup_title_2))
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
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(text = stringResource(R.string.setup_qr_hint), style = WalleeTextStyles.footnote)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = userId,
                    onValueChange = { userId = it.filter(Char::isDigit) },
                    label = stringResource(R.string.setup_user_id),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = authKey,
                    onValueChange = { authKey = it },
                    label = stringResource(R.string.setup_auth_key),
                    password = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(text = stringResource(R.string.setup_help), style = WalleeTextStyles.label)
                WTextButton(text = stringResource(R.string.setup_help_link), onClick = {})
                Spacer(Modifier.height(WalleeSpacing.S2))
                WPrimaryButton(
                    text = stringResource(R.string.setup_test_and_save),
                    onClick = onContinue,
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(text = stringResource(R.string.setup_placeholder_note), style = WalleeTextStyles.footnote)
                Spacer(Modifier.height(WalleeSpacing.S4))
            }
        }
    }
}
