package com.wallee.terminallinker.feature.terminals

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.SpaceChip
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.WBadge
import com.wallee.terminallinker.core.ui.components.WBadgeKind
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WPullToRefresh
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WSegment
import com.wallee.terminallinker.core.ui.components.WSegmented
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.core.update.UpdateInfo
import com.wallee.terminallinker.di.appViewModel
import com.wallee.terminallinker.di.appViewModelWithState
import com.wallee.terminallinker.feature.spaces.AddSpaceDialog
import com.wallee.terminallinker.feature.spaces.SpaceSheet
import com.wallee.terminallinker.feature.spaces.SpacesViewModel

/** Home screen (docs/03 §Terminalliste): live list of the active space with local search, filter and sort. */
@Composable
fun TerminalsScreen(
    onOpenTerminal: (Long) -> Unit,
    onQuickLink: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onScanSpaces: () -> Unit,
    spacesViewModel: SpacesViewModel = appViewModel { SpacesViewModel(it) },
    viewModel: TerminalsViewModel =
        appViewModelWithState { container, handle -> TerminalsViewModel(container, handle) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val update by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var sortOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { spacesViewModel.consumeScannedSpaceIds() }
    val spaceName = state.space?.name ?: stringResource(R.string.space_none_active)

    WScreen(
        header = {
            WHeader(
                leading = {
                    SpaceChip(name = spaceName, onClick = {
                        sheetOpen = true
                    }, modifier = Modifier.widthIn(max = 220.dp))
                },
            )
        },
    ) {
        update?.let { info ->
            UpdateBanner(
                info = info,
                onOpen = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, info.url.toUri()))
                    }
                },
                onDismiss = viewModel::dismissUpdate,
            )
        }
        Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side)) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            Headline(line1 = stringResource(R.string.terminals_title_1), line2 = spaceName)
            Spacer(Modifier.height(WalleeSpacing.S2))
            if (state.space == null) {
                Text(text = stringResource(R.string.space_choose_first), style = WalleeTextStyles.body)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WPrimaryButton(text = stringResource(R.string.space_choose), onClick = {
                    sheetOpen = true
                }, large = true)
            } else {
                WInput(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.terminals_search_hint),
                    leadingIconRes = R.drawable.ic_search,
                    trailing = if (state.query.isNotEmpty()) {
                        {
                            WIconButton(
                                iconRes = R.drawable.ic_close,
                                contentDescription = stringResource(R.string.cd_clear_search),
                                onClick = viewModel::clearQuery,
                                tint = WalleeColors.TextMuted,
                            )
                        }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
            }
        }
        if (state.space != null) {
            WSegmented(
                segments = listOf(
                    WSegment(stringResource(R.string.terminals_filter_all), state.counts.all),
                    WSegment(stringResource(R.string.terminals_filter_unlinked), state.counts.unlinked),
                    WSegment(stringResource(R.string.terminals_filter_linked), state.counts.linked),
                ),
                selectedIndex = state.filter.ordinal,
                onSelect = { viewModel.setFilter(TerminalFilter.entries[it]) },
            )
            WTextButton(
                text = stringResource(R.string.terminals_sort_label, stringResource(state.sort.labelRes())),
                onClick = { sortOpen = true },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            WPullToRefresh(
                refreshing = state.loading && state.loaded,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (!state.loaded && state.loading) {
                        item { LoadingRow() }
                    }
                    if (!state.loaded && state.error != null) {
                        item { ErrorPanel(state.error!!, onRetry = viewModel::refresh) }
                    }
                    items(state.rows, key = { it.id }) { terminal ->
                        TerminalRow(
                            terminal,
                            onClick = { onOpenTerminal(terminal.id) },
                            onQuickLink = if (terminal.canQuickLink) {
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onQuickLink(terminal.id)
                                }
                            } else {
                                null
                            },
                        )
                    }
                    item { ListFooter(state, onClearQuery = viewModel::clearQuery) }
                }
            }
        }
    }
    if (sheetOpen) {
        SpaceSheet(
            viewModel = spacesViewModel,
            onDismiss = { sheetOpen = false },
            onOpenSettings = {
                sheetOpen = false
                onOpenSettings()
            },
        )
    }
    if (sortOpen) {
        SortSheet(current = state.sort, onSelect = {
            viewModel.setSort(it)
            sortOpen = false
        }, onDismiss = {
            sortOpen =
                false
        })
    }
    AddSpaceDialog(viewModel = spacesViewModel, onScanSpaces = onScanSpaces)
}

/** Unlinked, active terminals get the long-press quick action (docs/03 §Terminalliste, Schnellaktion). */
private val PaymentTerminal.canQuickLink: Boolean
    get() = !linked && !isDecommissioned && state == "ACTIVE"

@Composable
private fun TerminalRow(terminal: PaymentTerminal, onClick: () -> Unit, onQuickLink: (() -> Unit)?) {
    val serial = terminal.deviceSerialNumber?.let { stringResource(R.string.terminal_serial_line, it) }
    WListRow(
        title = terminal.displayName,
        subtitle = listOfNotNull(
            terminal.identifier,
            serial ?: terminal.deviceName,
            terminal.locationName,
        ).joinToString(" · ").ifBlank {
            null
        },
        badge = { WBadge(kind = terminal.badgeKind()) },
        onClick = onClick,
        onLongClick = onQuickLink,
        longClickLabel = if (onQuickLink != null) stringResource(R.string.cd_quick_link) else null,
    )
}

/** Discreet grey strip under the header: "Version x.y available" + view + dismiss (docs/01 §Update-Hinweis). */
@Composable
private fun UpdateBanner(info: UpdateInfo, onOpen: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WalleeColors.BgSoft)
            .padding(start = WalleeSpacing.Side, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.update_banner, info.version),
            style = WalleeTextStyles.label,
            color = WalleeColors.Text,
            modifier = Modifier.weight(1f),
        )
        WTextButton(text = stringResource(R.string.update_banner_open), onClick = onOpen)
        WIconButton(
            iconRes = R.drawable.ic_close,
            contentDescription = stringResource(R.string.cd_dismiss_update),
            onClick = onDismiss,
            tint = WalleeColors.TextMuted,
        )
    }
}

@Composable
private fun LoadingRow() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(WalleeSpacing.S3),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Spinner()
        Spacer(Modifier.height(WalleeSpacing.S1))
        Text(text = stringResource(R.string.terminals_loading), style = WalleeTextStyles.label)
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(WalleeSpacing.Side)) {
        Text(text = stringResource(R.string.terminals_error_title), style = WalleeTextStyles.sectionTitle)
        Spacer(Modifier.height(WalleeSpacing.S1))
        Text(text = message, style = WalleeTextStyles.body)
        Spacer(Modifier.height(WalleeSpacing.S2))
        WSecondaryButton(text = stringResource(R.string.action_retry), onClick = onRetry)
    }
}

@Composable
private fun ListFooter(state: TerminalsUiState, onClearQuery: () -> Unit) {
    Column(modifier = Modifier.padding(WalleeSpacing.Side).navigationBarsPadding()) {
        if (state.loaded && state.rows.isEmpty()) {
            Text(
                text = if (state.query.isBlank()) {
                    stringResource(
                        R.string.terminals_empty,
                    )
                } else {
                    stringResource(R.string.terminals_no_match, state.query)
                },
                style = WalleeTextStyles.body,
            )
            if (state.query.isNotBlank()) {
                WTextButton(
                    text = stringResource(R.string.terminals_clear_search),
                    onClick = onClearQuery,
                )
            }
            Spacer(Modifier.height(WalleeSpacing.S1))
        }
        if (state.capped) {
            Text(
                text = stringResource(R.string.terminals_capped, TerminalRepository.MAX_TERMINALS),
                style = WalleeTextStyles.label,
                color = WalleeColors.OrangeText,
            )
            Spacer(Modifier.height(4.dp))
        }
        if (state.loaded && state.error != null) {
            Text(
                text = stringResource(R.string.terminals_offline_cache),
                style = WalleeTextStyles.label,
                color = WalleeColors.OrangeText,
            )
            Spacer(Modifier.height(4.dp))
        }
        state.loadedAtMillis?.let {
            Text(
                text = stringResource(R.string.terminals_status_line, state.counts.all, formatTime(it)),
                style = WalleeTextStyles.footnote,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(text = stringResource(R.string.terminals_placeholder_note), style = WalleeTextStyles.footnote)
    }
}

@Composable
private fun SortSheet(current: TerminalSort, onSelect: (TerminalSort) -> Unit, onDismiss: () -> Unit) {
    WBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.terminals_sort_title),
                style = WalleeTextStyles.sectionTitle,
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S1))
            TerminalSort.entries.forEach { sort ->
                WListRow(
                    title = stringResource(sort.labelRes()),
                    onClick = { onSelect(sort) },
                    showChevron = false,
                    trailing = if (sort == current) {
                        {
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.cd_selected),
                                tint = WalleeColors.Black,
                                modifier = Modifier.padding(0.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            Spacer(Modifier.height(WalleeSpacing.S2))
        }
    }
}

fun TerminalSort.labelRes(): Int = when (this) {
    TerminalSort.UNLINKED_FIRST -> R.string.terminals_sort_unlinked_first
    TerminalSort.NAME -> R.string.terminals_sort_name
    TerminalSort.IDENTIFIER -> R.string.terminals_sort_identifier
    TerminalSort.LOCATION -> R.string.terminals_sort_location
    TerminalSort.ACTIVATED -> R.string.terminals_sort_activated
}

fun PaymentTerminal.badgeKind(): WBadgeKind = when {
    isDecommissioned -> WBadgeKind.Decommissioned
    state == "INACTIVE" -> WBadgeKind.Inactive
    state == "PREPARING" || state == "CREATE" -> WBadgeKind.Preparing
    linked -> WBadgeKind.Linked
    else -> WBadgeKind.Unlinked
}
