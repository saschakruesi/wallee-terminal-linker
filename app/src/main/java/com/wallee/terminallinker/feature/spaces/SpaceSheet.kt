package com.wallee.terminallinker.feature.spaces

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Hairline
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.feature.terminals.SampleTerminals

/** Space picker sheet from docs/03 — phase 1 shows the sample space only; the repository comes in phase 2. */
@Composable
fun SpaceSheet(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    WBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.space_choose),
            style = WalleeTextStyles.sectionTitle,
            modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
        )
        Spacer(Modifier.height(WalleeSpacing.S2))
        Text(
            text = stringResource(R.string.space_recent),
            style = WalleeTextStyles.label,
            modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
        )
        WListRow(
            title = SampleTerminals.SPACE_NAME,
            subtitle = SampleTerminals.SPACE_ID.toString(),
            onClick = onDismiss,
            showChevron = false,
            trailing = {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = WalleeColors.Black,
                    modifier = Modifier.size(WalleeSize.Icon),
                )
            },
        )
        WListRow(
            title = "Café Seeblick",
            subtitle = "23456",
            onClick = onDismiss,
            showChevron = false,
        )
        WListRow(
            title = "Praxis Dr. Beispiel",
            subtitle = "34567",
            onClick = null,
            showChevron = false,
            trailing = { Text(text = stringResource(R.string.space_inactive), style = WalleeTextStyles.label) },
        )
        Hairline()
        WListRow(
            title = stringResource(R.string.space_add_manual),
            onClick = onDismiss,
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
        Spacer(Modifier.height(WalleeSpacing.S2).navigationBarsPadding())
    }
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
