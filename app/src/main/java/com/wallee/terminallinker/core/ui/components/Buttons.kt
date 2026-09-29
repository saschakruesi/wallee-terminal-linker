package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.ControlShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles

private const val DISABLED_ALPHA = 0.4f

/**
 * Black primary button, 44 dp (or 52 dp full width when [large]). On a turquoise panel it inverts to white
 * with black text. [loading] swaps the label for a 20 dp spinner while keeping the size, and disables the
 * button.
 */
@Composable
fun WPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    large: Boolean = false,
    onTurquoise: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = when {
        onTurquoise && pressed -> WalleeColors.BgSoft
        onTurquoise -> WalleeColors.White
        pressed -> WalleeColors.PrimaryPressed
        else -> WalleeColors.Black
    }
    val foreground = if (onTurquoise) WalleeColors.Black else WalleeColors.White
    val active = enabled && !loading
    val loadingDescription = stringResourceCompat(R.string.cd_loading)
    Box(
        modifier = modifier
            .then(if (large) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = if (large) WalleeSize.BigAction else WalleeSize.Control)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(ControlShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = if (onTurquoise) WalleeColors.Black else WalleeColors.White),
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { if (loading) stateDescription = loadingDescription }
            .padding(horizontal = WalleeSpacing.S3, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            Spinner(size = 20.dp, color = foreground)
        } else {
            Text(
                text = text,
                style = WalleeTextStyles.button,
                color = foreground,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Transparent button with a 1 dp hairline border and black text; pressed `#F7F7F7`.
 * [onDark] switches to a white border and white text for black backgrounds (scanner).
 * An optional [iconRes] is drawn 24 dp before the label.
 */
@Composable
fun WSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    large: Boolean = false,
    onDark: Boolean = false,
    iconRes: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val foreground = if (onDark) WalleeColors.White else WalleeColors.Black
    val background = when {
        !pressed -> Color.Transparent
        onDark -> WalleeColors.White.copy(alpha = 0.15f)
        else -> WalleeColors.BgSoft
    }
    Box(
        modifier = modifier
            .then(if (large) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = if (large) WalleeSize.BigAction else WalleeSize.Control)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(ControlShape)
            .background(background)
            .border(WalleeSize.Hairline, if (onDark) WalleeColors.White else WalleeColors.Line, ControlShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = foreground),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = WalleeSpacing.S2, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(WalleeSize.Icon),
                )
                Spacer(Modifier.width(WalleeSpacing.S1))
            }
            Text(
                text = text,
                style = WalleeTextStyles.button,
                color = foreground,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Text-only action in `TurquoiseText`, or `Orange` when [destructive]. No underline, 44 dp touch height. */
@Composable
fun WTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onDark: Boolean = false,
) {
    val color = when {
        onDark -> WalleeColors.White
        destructive -> WalleeColors.Orange
        else -> WalleeColors.TurquoiseText
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = WalleeSize.Control)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(ControlShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = color),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = WalleeSpacing.S1),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = WalleeTextStyles.button, color = color, maxLines = 2, textAlign = TextAlign.Center)
    }
}

/** 44 dp icon-only button with unbounded ripple. Always pass a [contentDescription] for TalkBack. */
@Composable
fun WIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = WalleeColors.Black,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(WalleeSize.Control)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = 22.dp, color = tint),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(WalleeSize.Icon),
        )
    }
}
