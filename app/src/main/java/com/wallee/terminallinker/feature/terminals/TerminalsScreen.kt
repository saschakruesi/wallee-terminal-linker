package com.wallee.terminallinker.feature.terminals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.SpaceChip
import com.wallee.terminallinker.core.ui.components.WBadge
import com.wallee.terminallinker.core.ui.components.WBadgeKind
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSegment
import com.wallee.terminallinker.core.ui.components.WSegmented
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.feature.spaces.SpaceSheet

/** Home screen layout from docs/03 with phase-1 sample data; search and filters already work locally. */
@Composable
fun TerminalsScreen(onOpenTerminal: (Long) -> Unit, onOpenSettings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val all = SampleTerminals.items
    val filtered = all
        .filter { t ->
            query.isBlank() ||
                listOfNotNull(t.name, t.identifier, t.deviceSerialNumber, t.location)
                    .any { it.contains(query, ignoreCase = true) }
        }
        .filter { t ->
            when (filter) {
                1 -> !t.linked
                2 -> t.linked
                else -> true
            }
        }
        .sortedWith(compareBy<SampleTerminal> { it.linked }.thenBy { it.name.lowercase() })

    WScreen(
        header = {
            WHeader(
                leading = {
                    SpaceChip(
                        name = SampleTerminals.SPACE_NAME,
                        onClick = { sheetOpen = true },
                        modifier = Modifier.widthIn(max = 220.dp),
                    )
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side)) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            Headline(line1 = stringResource(R.string.terminals_title_1), line2 = SampleTerminals.SPACE_NAME)
            Spacer(Modifier.height(WalleeSpacing.S2))
            WInput(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.terminals_search_hint),
                leadingIconRes = R.drawable.ic_search,
            )
            Spacer(Modifier.height(WalleeSpacing.S1))
        }
        WSegmented(
            segments = listOf(
                WSegment(stringResource(R.string.terminals_filter_all), all.size),
                WSegment(stringResource(R.string.terminals_filter_unlinked), all.count { !it.linked }),
                WSegment(stringResource(R.string.terminals_filter_linked), all.count { it.linked }),
            ),
            selectedIndex = filter,
            onSelect = { filter = it },
        )
        WTextButton(
            text = stringResource(
                R.string.terminals_sort_label,
                stringResource(R.string.terminals_sort_unlinked_first),
            ),
            onClick = {},
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(filtered, key = { it.id }) { t ->
                WListRow(
                    title = t.name,
                    subtitle = listOfNotNull(
                        t.identifier,
                        if (t.linked) "S/N ${t.deviceSerialNumber}" else t.deviceName,
                        t.location,
                    ).joinToString(" · "),
                    badge = { WBadge(kind = t.badgeKind()) },
                    onClick = { onOpenTerminal(t.id) },
                )
            }
            item {
                if (filtered.isEmpty()) {
                    Column(modifier = Modifier.padding(WalleeSpacing.Side)) {
                        Text(
                            text = if (query.isBlank()) {
                                stringResource(R.string.terminals_empty)
                            } else {
                                stringResource(R.string.terminals_no_match, query)
                            },
                            style = WalleeTextStyles.body,
                        )
                        if (query.isNotBlank()) {
                            WTextButton(text = stringResource(R.string.terminals_clear_search), onClick = {
                                query = ""
                            })
                        }
                    }
                }
                Column(modifier = Modifier.padding(WalleeSpacing.Side)) {
                    Text(
                        text = stringResource(R.string.terminals_status_line, all.size, "14:32"),
                        style = WalleeTextStyles.footnote,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = stringResource(R.string.terminals_placeholder_note), style = WalleeTextStyles.footnote)
                }
            }
        }
    }
    if (sheetOpen) {
        SpaceSheet(
            onDismiss = { sheetOpen = false },
            onOpenSettings = {
                sheetOpen = false
                onOpenSettings()
            },
        )
    }
}

fun SampleTerminal.badgeKind(): WBadgeKind = when {
    state == "DECOMMISSIONING" || state == "DECOMMISSIONED" -> WBadgeKind.Decommissioned
    state == "INACTIVE" -> WBadgeKind.Inactive
    state == "PREPARING" || state == "CREATE" -> WBadgeKind.Preparing
    linked -> WBadgeKind.Linked
    else -> WBadgeKind.Unlinked
}
