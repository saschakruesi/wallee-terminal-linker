package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.wallee.terminallinker.core.ui.WalleeTextStyles

/**
 * The wallee headline signature: two lines of 24 sp, hierarchy by weight and colour only.
 * Line 1 is the rubric (Light, grey), line 2 the subject (Medium, black).
 */
@Composable
fun Headline(
    line1: String,
    line2: String,
    modifier: Modifier = Modifier,
    line2Trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() },
    ) {
        Text(text = line1, style = WalleeTextStyles.headline1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (line2Trailing == null) {
            Text(text = line2, style = WalleeTextStyles.headline2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = line2,
                    style = WalleeTextStyles.headline2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                line2Trailing()
            }
        }
    }
}
