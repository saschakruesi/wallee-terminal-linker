package com.wallee.terminallinker.feature.link

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.TurquoisePanel
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.di.appContainer
import com.wallee.terminallinker.feature.terminals.displayName
import com.wallee.terminallinker.navigation.ResultOutcome
import com.wallee.terminallinker.navigation.ResultRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Result (docs/03 §Ergebnis): turquoise upper 60 % when the server confirmed the new state; a white warning
 * panel with "Aktualisieren" when the reload after the 204 did not yet show it.
 */
@Composable
fun ResultScreen(route: ResultRoute, onBackToList: () -> Unit, onLinkNext: () -> Unit) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var terminal by remember { mutableStateOf<PaymentTerminal?>(null) }
    var confirmed by remember { mutableStateOf(route.confirmed) }
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(route.terminalId) {
        val spaceId = container.spaceRepository.activeSpace.first()?.id
        terminal = spaceId?.let { container.terminalRepository.cached(it, route.terminalId) }
    }
    val name = terminal?.displayName ?: route.terminalId.toString()
    val identifier = terminal?.identifier ?: ""

    fun refresh() {
        scope.launch {
            refreshing = true
            val spaceId = container.spaceRepository.activeSpace.first()?.id
            val fresh = spaceId?.let {
                runCatching { container.terminalRepository.get(it, route.terminalId) }.getOrNull()
            }
            if (fresh != null) {
                terminal = fresh
                confirmed = when (route.outcome) {
                    ResultOutcome.UNLINKED -> !fresh.linked
                    else -> fresh.deviceSerialNumber.equals(route.serial, ignoreCase = true)
                }
            }
            refreshing = false
        }
    }

    WScreen(applyStatusBarInset = false) {
        if (confirmed) {
            TurquoisePanel(showLogo = true, behindStatusBar = true, modifier = Modifier.weight(0.6f)) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_large),
                    contentDescription = null,
                    tint = WalleeColors.Black,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
                Text(
                    text = when (route.outcome) {
                        ResultOutcome.LINKED -> stringResource(R.string.result_linked)
                        ResultOutcome.REPLACED -> stringResource(R.string.result_replaced)
                        ResultOutcome.UNLINKED -> stringResource(R.string.result_unlinked)
                    },
                    style = WalleeTextStyles.statement,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(
                    text = if (route.outcome ==
                        ResultOutcome.REPLACED
                    ) {
                        name
                    } else {
                        "$name · $identifier"
                    },
                    style = WalleeTextStyles.body,
                    color = WalleeColors.Black,
                )
                when (route.outcome) {
                    ResultOutcome.LINKED -> route.serial?.let {
                        Text(
                            text = stringResource(R.string.result_serial_line, it),
                            style = WalleeTextStyles.value,
                            color = WalleeColors.Black,
                        )
                    }

                    ResultOutcome.REPLACED -> Text(
                        text = stringResource(
                            R.string.result_replaced_line,
                            route.serial ?: "",
                            route.previousSerial ?: "",
                        ),
                        style = WalleeTextStyles.value,
                        color = WalleeColors.Black,
                    )

                    ResultOutcome.UNLINKED -> Unit
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(WalleeSpacing.S5),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = WalleeColors.Orange,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
                Text(
                    text = stringResource(
                        if (route.outcome ==
                            ResultOutcome.UNLINKED
                        ) {
                            R.string.result_unconfirmed_unlink_text
                        } else {
                            R.string.result_unconfirmed_text
                        },
                    ),
                    style = WalleeTextStyles.body,
                )
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(text = "$name · $identifier", style = WalleeTextStyles.label)
                Spacer(Modifier.height(WalleeSpacing.S3))
                if (refreshing) {
                    Spinner()
                } else {
                    WSecondaryButton(
                        text = stringResource(R.string.result_refresh),
                        onClick = ::refresh,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .padding(WalleeSpacing.Side),
        ) {
            Spacer(Modifier.height(WalleeSpacing.S1))
            WPrimaryButton(text = stringResource(R.string.result_back_to_list), onClick = onBackToList, large = true)
            Spacer(Modifier.height(WalleeSpacing.S2))
            WSecondaryButton(text = stringResource(R.string.result_link_next), onClick = onLinkNext, large = true)
        }
    }
}
