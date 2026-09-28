package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeTextStyles

data class WSegment(val label: String, val count: Int? = null)

/** Text segments, 44 dp; active = black Medium with a 2 dp turquoise underline, inactive = muted grey. */
@Composable
fun WSegmented(segments: List<WSegment>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.selectableGroup()) {
            segments.forEachIndexed { index, segment ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(WalleeSize.Control)
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = ripple(color = WalleeColors.Turquoise),
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = segment.label,
                            style = WalleeTextStyles.body.copy(
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                color = if (selected) WalleeColors.Black else WalleeColors.TextMuted,
                            ),
                            maxLines = 1,
                        )
                        if (segment.count != null) {
                            Spacer(Modifier.width(4.dp))
                            Text(text = segment.count.toString(), style = WalleeTextStyles.label, maxLines = 1)
                        }
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(WalleeColors.Turquoise),
                        )
                    }
                }
            }
        }
        Hairline()
    }
}
