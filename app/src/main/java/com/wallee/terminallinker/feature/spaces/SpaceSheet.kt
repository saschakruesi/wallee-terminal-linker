package com.wallee.terminallinker.feature.spaces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Hairline
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WDialog
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WSecondaryButton

private const val SEARCH_THRESHOLD = 8

/** Space picker (docs/03 §Space-Auswahl): recent, all A–Z, inactive greyed, manual add, settings. */
@Composable
fun SpaceSheet(viewModel: SpacesViewModel, onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = if (query.isBlank()) {
        spaces
    } else {
        spaces.filter {
            it.name.contains(query, true) ||
                it.id.toString().contains(query)
        }
    }

    WBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.space_choose),
                style = WalleeTextStyles.sectionTitle,
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            if (spaces.size > SEARCH_THRESHOLD) {
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.space_search),
                    leadingIconRes = R.drawable.ic_search,
                    modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
                )
            }
            Spacer(Modifier.height(WalleeSpacing.S2))
            if (spaces.isEmpty()) {
                Text(
                    text = stringResource(R.string.space_choose_first),
                    style = WalleeTextStyles.body,
                    modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
            }
            if (query.isBlank() && recent.isNotEmpty() && spaces.size > 1) {
                SectionLabel(stringResource(R.string.space_recent))
                recent.forEach {
                    SpaceRow(it, it.id == active?.id) {
                        viewModel.select(it.id)
                        onDismiss()
                    }
                }
                Spacer(Modifier.height(WalleeSpacing.S1))
                SectionLabel(stringResource(R.string.space_all))
            }
            filtered.forEach { space ->
                SpaceRow(space, space.id == active?.id) {
                    viewModel.select(space.id)
                    onDismiss()
                }
            }
            Hairline()
            WListRow(
                title = stringResource(R.string.space_add_manual),
                onClick = viewModel::openAddSpace,
                showChevron = false,
                hairline = false,
                leading = { SheetIcon(R.drawable.ic_plus) },
            )
            WListRow(
                title = stringResource(R.string.space_settings),
                onClick = onOpenSettings,
                showChevron = false,
                hairline = false,
                leading = { SheetIcon(R.drawable.ic_settings) },
            )
            Spacer(Modifier.height(WalleeSpacing.S2))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = WalleeTextStyles.label, modifier = Modifier.padding(horizontal = WalleeSpacing.Side))
}

@Composable
private fun SpaceRow(space: SpaceRef, selected: Boolean, onSelect: () -> Unit) {
    WListRow(
        title = space.name,
        subtitle = listOfNotNull(space.id.toString(), space.accountName).joinToString(" · "),
        onClick = if (space.active) onSelect else null,
        showChevron = false,
        trailing = when {
            !space.active -> {
                { Text(text = stringResource(R.string.space_inactive), style = WalleeTextStyles.label) }
            }

            selected -> {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.cd_selected),
                        tint = WalleeColors.Black,
                        modifier = Modifier.size(WalleeSize.Icon),
                    )
                }
            }

            else -> null
        },
    )
}

@Composable
private fun SheetIcon(res: Int) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = WalleeColors.Black,
        modifier = Modifier.size(WalleeSize.Icon),
    )
}

/** "Space-ID manuell hinzufügen": ID → Prüfen (`GET /spaces/{id}`) → name → Hinzufügen. QR icon scans space IDs only. */
@Composable
fun AddSpaceDialog(viewModel: SpacesViewModel, onScanSpaces: () -> Unit, activateOnAdd: Boolean = true) {
    val state by viewModel.addSpace.collectAsStateWithLifecycle()
    if (!state.open) return
    WDialog(
        title = stringResource(R.string.space_add_title),
        text = stringResource(R.string.space_add_hint),
        onDismissRequest = viewModel::closeAddSpace,
        dismissButton = {
            WSecondaryButton(text = stringResource(R.string.action_cancel), onClick = viewModel::closeAddSpace)
        },
        confirmButton = {
            WPrimaryButton(
                text = stringResource(R.string.setup_add),
                onClick = { viewModel.confirmAddSpace(activateOnAdd) },
                enabled = state.verified != null,
            )
        },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            WInput(
                value = state.id,
                onValueChange = viewModel::onAddSpaceIdChange,
                label = stringResource(R.string.setup_manual_space_id),
                enabled = !state.checking,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.verifyAddSpace() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(WalleeSpacing.S1))
            WIconButton(
                iconRes = R.drawable.ic_qr_scan,
                contentDescription = stringResource(R.string.cd_scan_qr),
                onClick = onScanSpaces,
            )
        }
        Spacer(Modifier.height(WalleeSpacing.S1))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.checking) {
                Spinner(modifier = Modifier.padding(12.dp))
            } else {
                WSecondaryButton(
                    text = stringResource(R.string.setup_check),
                    onClick = viewModel::verifyAddSpace,
                    enabled = state.id.isNotBlank(),
                )
            }
            Spacer(Modifier.width(WalleeSpacing.S2))
            state.verified?.let {
                Text(
                    text = stringResource(R.string.setup_verified_space, it.name, it.id),
                    style = WalleeTextStyles.bodyTitle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        state.error?.let {
            Spacer(Modifier.height(4.dp))
            Text(text = it, style = WalleeTextStyles.label, color = WalleeColors.Orange)
        }
        Spacer(Modifier.height(WalleeSpacing.S1))
        Spacer(Modifier.fillMaxWidth())
    }
}
