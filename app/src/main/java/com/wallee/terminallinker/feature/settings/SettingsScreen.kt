package com.wallee.terminallinker.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WScreen

/** Settings skeleton from docs/03; sections are placeholders until phases 2 and 5. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenStyleguide: (() -> Unit)?) {
    WScreen(header = { WHeader(leading = { WBackButton(onClick = onBack) }) }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            Headline(
                line1 = stringResource(R.string.space_settings),
                line2 = stringResource(R.string.settings_title_2),
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S3))
            WListRow(title = stringResource(R.string.settings_section_credentials), onClick = {})
            WListRow(title = stringResource(R.string.settings_section_spaces), onClick = {})
            WListRow(title = stringResource(R.string.settings_section_display), onClick = {})
            WListRow(
                title = stringResource(R.string.settings_section_about),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                onClick = {},
            )
            if (onOpenStyleguide != null) {
                WListRow(title = stringResource(R.string.settings_styleguide), onClick = onOpenStyleguide)
            }
            Spacer(Modifier.height(WalleeSpacing.S2))
            Text(
                text = stringResource(R.string.settings_placeholder_note),
                style = WalleeTextStyles.footnote,
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
        }
    }
}
