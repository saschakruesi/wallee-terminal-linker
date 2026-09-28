package com.wallee.terminallinker.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.di.appContainer
import com.wallee.terminallinker.di.appViewModel
import com.wallee.terminallinker.feature.spaces.AddSpaceDialog
import com.wallee.terminallinker.feature.spaces.SpaceMode
import com.wallee.terminallinker.feature.spaces.SpacesViewModel

/** Settings (docs/03): credentials and spaces are live since phase 2; display and about follow in phase 5. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditCredentials: () -> Unit,
    onScanSpaces: () -> Unit,
    onOpenStyleguide: (() -> Unit)?,
    spacesViewModel: SpacesViewModel = appViewModel { SpacesViewModel(it) },
) {
    val container = appContainer()
    val credentials by container.credentialStore.credentials.collectAsStateWithLifecycle()
    val mode by spacesViewModel.mode.collectAsStateWithLifecycle()
    val manual by spacesViewModel.manual.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { spacesViewModel.consumeScannedSpaceIds() }

    WScreen(header = { WHeader(leading = { WBackButton(onClick = onBack) }) }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            Headline(
                line1 = stringResource(R.string.space_settings),
                line2 = stringResource(R.string.settings_title_2),
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S3))
            Section(stringResource(R.string.settings_section_credentials))
            WListRow(
                title = credentials?.let { stringResource(R.string.settings_credentials_value, it.userId.toString()) }
                    ?: stringResource(R.string.error_no_credentials),
                subtitle = if (credentials != null &&
                    !container.credentialStore.isPersisted()
                ) {
                    stringResource(R.string.settings_credentials_memory_only)
                } else {
                    null
                },
                showChevron = false,
                trailing = {
                    WTextButton(text = stringResource(R.string.settings_change), onClick = onEditCredentials)
                },
            )
            Spacer(Modifier.height(WalleeSpacing.S2))
            Section(stringResource(R.string.settings_section_spaces))
            WListRow(
                title = stringResource(
                    R.string.settings_spaces_mode,
                    stringResource(
                        if (mode ==
                            SpaceMode.AUTO
                        ) {
                            R.string.space_mode_auto
                        } else {
                            R.string.space_mode_manual
                        },
                    ),
                ),
                subtitle = stringResource(R.string.settings_manual_spaces),
                showChevron = false,
            )
            if (manual.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_manual_spaces),
                    style = WalleeTextStyles.label,
                    modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S1),
                )
            }
            manual.forEach { space ->
                WListRow(
                    title = space.name,
                    subtitle = space.id.toString(),
                    showChevron = false,
                    trailing = {
                        WTextButton(text = stringResource(R.string.space_remove), onClick = {
                            spacesViewModel.removeManual(space.id)
                        }, destructive = true)
                    },
                )
            }
            WListRow(title = stringResource(R.string.settings_add_space), onClick = spacesViewModel::openAddSpace)
            Spacer(Modifier.height(WalleeSpacing.S2))
            Section(stringResource(R.string.settings_section_display))
            WListRow(title = stringResource(R.string.settings_section_display), onClick = {})
            Section(stringResource(R.string.settings_section_about))
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
            Spacer(Modifier.height(WalleeSpacing.S4))
        }
    }
    AddSpaceDialog(viewModel = spacesViewModel, onScanSpaces = onScanSpaces, activateOnAdd = false)
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = WalleeTextStyles.sectionTitle,
        modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S1),
    )
}
