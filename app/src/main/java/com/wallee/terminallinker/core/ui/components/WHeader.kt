package com.wallee.terminallinker.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing

/**
 * 56 dp white header with hairline below. [leading] holds the space chip or the back button; the
 * turquoise wordmark sits top right at 22 dp height with ≥ 22 dp horizontal clearance.
 */
@Composable
fun WHeader(modifier: Modifier = Modifier, leading: @Composable () -> Unit = {}) {
    Column(modifier = modifier.fillMaxWidth().background(WalleeColors.Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WalleeSize.Header)
                .padding(start = WalleeSpacing.Side, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                leading()
            }
            WalleeLogo(modifier = Modifier.padding(start = 22.dp))
        }
        Hairline()
    }
}

enum class LogoVariant(val drawableRes: Int) {
    Turquoise(R.drawable.wallee_logo_turquoise),
    White(R.drawable.wallee_logo_white),
    Black(R.drawable.wallee_logo_black),
}

/** The wordmark is always the drawable, never text. Aspect ratio is preserved; only the height is set. */
@Composable
fun WalleeLogo(
    modifier: Modifier = Modifier,
    variant: LogoVariant = LogoVariant.Turquoise,
    height: Dp = WalleeSize.LogoHeader,
) {
    Image(
        painter = painterResource(variant.drawableRes),
        contentDescription = stringResource(R.string.cd_wallee_logo),
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height),
    )
}

/** Back button for sub-pages; offset so the glyph aligns with the 20 dp page margin. */
@Composable
fun WBackButton(onClick: () -> Unit) {
    WIconButton(
        iconRes = R.drawable.ic_back,
        contentDescription = stringResource(R.string.action_back),
        onClick = onClick,
        modifier = Modifier.offset(x = (-10).dp),
    )
}
