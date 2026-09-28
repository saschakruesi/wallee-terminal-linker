package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeTextStyles

/** Label (13 grey, left) / value (15, right, tabular figures) row, 48 dp, hairline below. */
@Composable
fun WFactRow(label: String, modifier: Modifier = Modifier, hairline: Boolean = true, value: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = WalleeSize.FactRow)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = WalleeTextStyles.label, modifier = Modifier.weight(0.42f))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(0.58f), contentAlignment = Alignment.CenterEnd) {
                value()
            }
        }
        if (hairline) Hairline()
    }
}

@Composable
fun WFactRow(label: String, value: String, modifier: Modifier = Modifier, hairline: Boolean = true) {
    WFactRow(label = label, modifier = modifier, hairline = hairline) {
        Text(text = value, style = WalleeTextStyles.value, textAlign = TextAlign.End)
    }
}

/** Convenience: a list of label/value pairs as rows with hairlines. */
@Composable
fun WFactTable(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        rows.forEachIndexed { index, (label, value) ->
            WFactRow(label = label, value = value, hairline = index < rows.lastIndex)
        }
    }
}
