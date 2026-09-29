package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles

/**
 * 64 dp list row: title (Medium 15 black), optional second line (13 grey), optional [badge] slot,
 * chevron when tappable. Pressed `#CFF7F4`, hairline below. Leading slot for icons or radio marks.
 * [onLongClick] adds a long-press action (quick link in the terminal list); [longClickLabel] names it for TalkBack.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
    showChevron: Boolean = onClick != null,
    leading: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    hairline: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (pressed) WalleeColors.Turquoise20 else Color.Transparent)
                .then(
                    if (onClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onLongClickLabel = longClickLabel,
                            onLongClick = onLongClick,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .defaultMinSize(minHeight = WalleeSize.ListRow)
                .padding(horizontal = WalleeSpacing.Side, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = WalleeTextStyles.bodyTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = WalleeTextStyles.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                badge()
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
            if (showChevron) {
                Spacer(Modifier.width(WalleeSpacing.S1))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = WalleeColors.TextMuted,
                    modifier = Modifier.size(WalleeSize.Icon),
                )
            }
        }
        if (hairline) Hairline()
    }
}
