package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.ControlShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles

/**
 * 44 dp single-line input with the label *above* the field (no floating label). Hairline border, 2 dp
 * turquoise border on focus, orange border plus 13 sp orange message on [error]. [password] adds the
 * eye toggle. [leadingIconRes] draws a 24 dp icon at the start (search field).
 */
@Composable
fun WInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    password: Boolean = false,
    leadingIconRes: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var revealed by rememberSaveable { mutableStateOf(false) }
    val borderWidth = if (focused) 2.dp else WalleeSize.Hairline
    val borderColor = when {
        error != null -> WalleeColors.Orange
        focused -> WalleeColors.Turquoise
        else -> WalleeColors.Line
    }
    Column(modifier = modifier.alpha(if (enabled) 1f else 0.5f)) {
        if (label != null) {
            Text(text = label, style = WalleeTextStyles.label)
            Spacer(Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WalleeSize.Control)
                .background(WalleeColors.Bg, ControlShape)
                .border(borderWidth, borderColor, ControlShape)
                .then(if (error != null) Modifier.semantics { this.error(error) } else Modifier)
                .padding(start = if (leadingIconRes != null) WalleeSpacing.S1 else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIconRes != null) {
                Icon(
                    painter = painterResource(leadingIconRes),
                    contentDescription = null,
                    tint = WalleeColors.TextMuted,
                    modifier = Modifier.size(WalleeSize.Icon),
                )
                Spacer(Modifier.width(WalleeSpacing.S1))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (password || trailing != null) 0.dp else 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = WalleeTextStyles.body,
                        color = WalleeColors.TextMuted,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    textStyle = WalleeTextStyles.body,
                    singleLine = true,
                    interactionSource = interaction,
                    cursorBrush = SolidColor(WalleeColors.Black),
                    visualTransformation = if (password &&
                        !revealed
                    ) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                )
            }
            if (password) {
                WIconButton(
                    iconRes = if (revealed) R.drawable.ic_eye_off else R.drawable.ic_eye,
                    contentDescription = stringResourceCompat(
                        if (revealed) R.string.cd_hide_password else R.string.cd_show_password,
                    ),
                    onClick = { revealed = !revealed },
                    tint = WalleeColors.TextMuted,
                    enabled = enabled,
                )
            } else if (trailing != null) {
                trailing()
            }
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = error, style = WalleeTextStyles.label, color = WalleeColors.Orange)
        }
    }
}
