package com.wallee.terminallinker.feature.settings

import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.prefs.AppLanguage
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WCheckbox
import com.wallee.terminallinker.core.ui.components.WConfirmDialog
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSegment
import com.wallee.terminallinker.core.ui.components.WSegmented
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.core.update.UpdateChecker
import com.wallee.terminallinker.di.appContainer
import com.wallee.terminallinker.di.appViewModel
import com.wallee.terminallinker.feature.spaces.AddSpaceDialog
import com.wallee.terminallinker.feature.spaces.SpaceMode
import com.wallee.terminallinker.feature.spaces.SpaceRef
import com.wallee.terminallinker.feature.spaces.SpacesViewModel
import com.wallee.terminallinker.feature.terminals.formatTime
import com.wallee.terminallinker.util.findActivity
import kotlinx.coroutines.launch

/** Settings (docs/03 §Einstellungen): credentials, spaces, display, about, delete all local data. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditCredentials: () -> Unit,
    onScanSpaces: () -> Unit,
    onOpenLicenses: () -> Unit,
    onWiped: () -> Unit,
    onOpenStyleguide: (() -> Unit)?,
    spacesViewModel: SpacesViewModel = appViewModel { SpacesViewModel(it) },
    viewModel: SettingsViewModel = appViewModel { SettingsViewModel(it) },
) {
    val context = LocalContext.current
    val container = appContainer()
    val credentials by container.credentialStore.credentials.collectAsStateWithLifecycle()
    val mode by spacesViewModel.mode.collectAsStateWithLifecycle()
    val spaces by spacesViewModel.spaces.collectAsStateWithLifecycle()
    val hidden by spacesViewModel.hidden.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastChecked by viewModel.lastUpdateCheckMillis.collectAsStateWithLifecycle()
    var removeCandidate by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmWipe by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { spacesViewModel.consumeScannedSpaceIds() }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

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
                subtitle = if (credentials != null && !container.credentialStore.isPersisted()) {
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
                subtitle = stringResource(R.string.settings_spaces_hint),
                showChevron = false,
            )
            if (spaces.isEmpty()) {
                Note(stringResource(R.string.settings_no_spaces))
            }
            spaces.forEach { space ->
                SpaceRow(space) {
                    WTextButton(text = stringResource(R.string.space_remove), onClick = {
                        removeCandidate = space.id
                    }, destructive = true)
                }
            }
            WListRow(title = stringResource(R.string.settings_add_space), onClick = spacesViewModel::openAddSpace)
            if (hidden.isNotEmpty()) {
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(
                    text = stringResource(R.string.settings_removed_spaces),
                    style = WalleeTextStyles.label,
                    modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S1),
                )
                hidden.forEach { space ->
                    SpaceRow(space) {
                        WTextButton(text = stringResource(R.string.settings_space_restore), onClick = {
                            spacesViewModel.restore(space.id)
                        })
                    }
                }
            }
            Spacer(Modifier.height(WalleeSpacing.S2))

            Section(stringResource(R.string.settings_section_display))
            Text(
                text = stringResource(R.string.settings_language),
                style = WalleeTextStyles.label,
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S1))
            WSegmented(
                segments = listOf(
                    WSegment(stringResource(R.string.settings_language_system)),
                    WSegment(stringResource(R.string.settings_language_de)),
                    WSegment(stringResource(R.string.settings_language_en)),
                ),
                selectedIndex = state.language.ordinal,
                onSelect = { index ->
                    val language = AppLanguage.entries[index]
                    if (language != state.language) {
                        viewModel.setLanguage(language)
                        // Resources are resolved in attachBaseContext; recreate applies the new language.
                        context.findActivity()?.recreate()
                    }
                },
            )
            Spacer(Modifier.height(WalleeSpacing.S2))
            val showDecommissioned by container.uiPrefs.showDecommissioned.collectAsStateWithLifecycle(
                initialValue = false,
            )
            WCheckbox(
                checked = showDecommissioned,
                onCheckedChange = { value -> scope.launch { container.uiPrefs.setShowDecommissioned(value) } },
                label = stringResource(R.string.settings_show_decommissioned),
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S2))

            Section(stringResource(R.string.settings_section_about))
            WListRow(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                showChevron = false,
            )
            val updateSubtitle = when {
                state.checkingUpdate -> stringResource(R.string.settings_update_checking)

                state.updateResult is UpdateCheckResult.Available -> stringResource(
                    R.string.settings_update_available,
                    (state.updateResult as UpdateCheckResult.Available).update.version,
                )

                state.updateResult is UpdateCheckResult.UpToDate -> stringResource(R.string.settings_update_none)

                state.updateResult is UpdateCheckResult.Failed -> stringResource(R.string.settings_update_failed)

                lastChecked != null -> stringResource(R.string.settings_update_last_checked, formatTime(lastChecked!!))

                else -> null
            }
            WListRow(
                title = stringResource(R.string.settings_check_updates),
                subtitle = updateSubtitle,
                onClick = {
                    val available = state.updateResult as? UpdateCheckResult.Available
                    if (available != null) openUrl(available.update.url) else viewModel.checkForUpdates()
                },
                showChevron = false,
                trailing = if (state.checkingUpdate) {
                    { Spinner() }
                } else {
                    null
                },
            )
            WListRow(
                title = stringResource(R.string.settings_release_page),
                onClick = { openUrl(UpdateChecker.RELEASES_PAGE) },
            )
            WListRow(title = stringResource(R.string.settings_licenses), onClick = onOpenLicenses)
            if (onOpenStyleguide != null) {
                WListRow(title = stringResource(R.string.settings_styleguide), onClick = onOpenStyleguide)
            }
            Spacer(Modifier.height(WalleeSpacing.S3))

            WTextButton(
                text = stringResource(R.string.settings_wipe),
                onClick = { confirmWipe = true },
                destructive = true,
                enabled = !state.wiping,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(WalleeSpacing.S4))
        }
    }
    AddSpaceDialog(viewModel = spacesViewModel, onScanSpaces = onScanSpaces, activateOnAdd = false)

    val candidate = (spaces + hidden).firstOrNull { it.id == removeCandidate }
    if (candidate != null) {
        WConfirmDialog(
            title = stringResource(R.string.settings_remove_space_title),
            text = stringResource(R.string.settings_remove_space_text, candidate.name, candidate.id.toString()),
            confirmText = stringResource(R.string.space_remove),
            onConfirm = {
                spacesViewModel.remove(candidate.id)
                removeCandidate = null
            },
            onDismiss = { removeCandidate = null },
            destructive = true,
        )
    }
    if (confirmWipe) {
        WConfirmDialog(
            title = stringResource(R.string.settings_wipe_title),
            text = stringResource(R.string.settings_wipe_text),
            confirmText = stringResource(R.string.settings_wipe_confirm),
            onConfirm = {
                confirmWipe = false
                viewModel.wipe(onDone = onWiped)
            },
            onDismiss = { confirmWipe = false },
            destructive = true,
        )
    }
}

@Composable
private fun SpaceRow(space: SpaceRef, trailing: @Composable () -> Unit) {
    WListRow(
        title = space.name,
        subtitle = listOfNotNull(
            space.id.toString(),
            stringResource(
                if (space.manual) R.string.settings_space_source_manual else R.string.settings_space_source_auto,
            ),
            if (!space.active) stringResource(R.string.space_inactive) else null,
        ).joinToString(" · "),
        showChevron = false,
        trailing = trailing,
    )
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = WalleeTextStyles.sectionTitle,
        modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S1),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = WalleeTextStyles.label,
        color = WalleeColors.TextMuted,
        modifier = Modifier.padding(horizontal = WalleeSpacing.Side, vertical = WalleeSpacing.S1),
    )
}
