package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.BadgeShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeTextStyles

enum class WBadgeKind(val background: Color, val foreground: Color, val labelRes: Int) {
    Linked(WalleeColors.Turquoise20, WalleeColors.TurquoiseText, R.string.badge_linked),
    Unlinked(WalleeColors.BadgeNeutral, WalleeColors.Text, R.string.badge_unlinked),
    Inactive(WalleeColors.BadgeNeutral, WalleeColors.TextMuted, R.string.badge_inactive),
    Preparing(WalleeColors.BadgeNeutral, WalleeColors.TextMuted, R.string.badge_preparing),
    Decommissioned(WalleeColors.BadgeNeutral, WalleeColors.TextMuted, R.string.badge_decommissioned),
    Error(WalleeColors.OrangeSoft, WalleeColors.OrangeText, R.string.badge_error),
}

/** 12 dp radius, 12 sp Medium, padding 2×8 dp. The badge always carries text — colour is never the only cue. */
@Composable
fun WBadge(kind: WBadgeKind, modifier: Modifier = Modifier, text: String = stringResource(kind.labelRes)) {
    Text(
        text = text,
        style = WalleeTextStyles.badge,
        color = kind.foreground,
        maxLines = 1,
        modifier = modifier
            .background(kind.background, BadgeShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
