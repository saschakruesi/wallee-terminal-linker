package com.wallee.terminallinker.feature.scan

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.CardShape
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.ViewfinderCorners
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.di.appViewModel
import com.wallee.terminallinker.feature.link.FailedPhase
import com.wallee.terminallinker.feature.link.LinkFlowState
import com.wallee.terminallinker.feature.link.LinkFlowViewModel
import com.wallee.terminallinker.feature.terminals.displayName
import com.wallee.terminallinker.navigation.ResultOutcome
import com.wallee.terminallinker.navigation.ScanMode
import com.wallee.terminallinker.util.findActivity

private val SERIAL_FINDER = Size(280f, 160f)
private val QR_FINDER = Size(240f, 240f)

/**
 * Scanner (docs/03 §Scanner): full-screen camera with viewfinder, torch, tap-to-focus, manual entry.
 * LINK/REPLACE drive [LinkFlowViewModel]; CREDENTIALS hands the QR payload to the credential flow.
 */
@Composable
fun ScanScreen(
    mode: ScanMode,
    terminalId: Long?,
    onClose: () -> Unit,
    onLinked: (
        terminalId: Long,
        outcome: ResultOutcome,
        serial: String,
        previousSerial: String?,
        confirmed: Boolean,
    ) -> Unit,
    onCredentialsScanned: () -> Unit,
    onToList: () -> Unit,
    spacesOnly: Boolean = false,
    scanner: BarcodeScanner = CameraBarcodeScanner,
) {
    val context = LocalContext.current
    LockPortrait()
    val credentials = mode == ScanMode.CREDENTIALS
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(Unit) { if (hasCamera && !granted && !denied) launcher.launch(Manifest.permission.CAMERA) }

    var torch by remember { mutableStateOf(false) }
    var manualOpen by rememberSaveable { mutableStateOf(false) }
    var manualValue by rememberSaveable { mutableStateOf("") }
    var credentialRaw by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current

    val linkViewModel: LinkFlowViewModel? =
        if (credentials || terminalId == null) null else appViewModel { LinkFlowViewModel(it, terminalId, mode) }
    val ui = linkViewModel?.ui?.collectAsStateWithLifecycle()?.value
    val flow = ui?.state ?: LinkFlowState.Scanning
    val terminal = ui?.terminal

    LaunchedEffect(flow) {
        when (flow) {
            is LinkFlowState.Confirm, is LinkFlowState.Invalid -> haptics.performHapticFeedback(
                HapticFeedbackType.LongPress,
            )

            is LinkFlowState.Done -> {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onLinked(terminalId!!, flow.outcome, flow.serial, flow.previousSerial, flow.confirmed)
            }

            else -> Unit
        }
    }

    val scanningPaused =
        (credentials && credentialRaw != null) || (!credentials && flow != LinkFlowState.Scanning) || manualOpen

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(WalleeColors.Black),
    ) {
        val density = LocalDensity.current
        val finderDp = if (credentials) QR_FINDER else SERIAL_FINDER
        val finderLeft = (maxWidth - finderDp.width.dp) / 2
        val finderTop = 140.dp
        val finderRect = remember(maxWidth, maxHeight, finderDp) {
            with(density) {
                android.graphics.Rect(
                    finderLeft.roundToPx(),
                    finderTop.roundToPx(),
                    (finderLeft + finderDp.width.dp).roundToPx(),
                    (finderTop + finderDp.height.dp).roundToPx(),
                )
            }
        }

        if (hasCamera && granted) {
            scanner.Content(
                formats = if (credentials) ScanFormats.QR_ONLY else ScanFormats.SERIAL,
                viewfinder = { finderRect },
                paused = scanningPaused,
                torch = torch,
                onDetected = { raw ->
                    if (credentials) credentialRaw = raw else linkViewModel?.onScanned(raw)
                },
                modifier = Modifier.fillMaxSize(),
            )
            Dim(finderLeft, finderTop, finderDp)
        }
        ViewfinderCorners(
            width = finderDp.width.dp,
            height = finderDp.height.dp,
            modifier = Modifier.offset(x = finderLeft, y = finderTop),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WalleeSpacing.S1, vertical = WalleeSpacing.S1),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.action_close),
                    onClick = onClose,
                    tint = WalleeColors.White,
                )
                if (hasCamera && granted) {
                    WIconButton(
                        iconRes = R.drawable.ic_flashlight,
                        contentDescription = stringResource(if (torch) R.string.cd_torch_off else R.string.cd_torch_on),
                        onClick = { torch = !torch },
                        tint = if (torch) WalleeColors.Turquoise else WalleeColors.White,
                    )
                }
            }
            Spacer(Modifier.height(finderTop + finderDp.height.dp - 56.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WalleeSpacing.Side),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(WalleeSpacing.S3))
                Text(
                    text = stringResource(
                        if (credentials) R.string.scan_hint_credentials else R.string.scan_hint_serial,
                    ),
                    style = WalleeTextStyles.body,
                    color = WalleeColors.White,
                    textAlign = TextAlign.Center,
                )
                if (terminal != null) {
                    Spacer(Modifier.height(WalleeSpacing.S1))
                    Text(
                        text = stringResource(
                            R.string.scan_terminal_line,
                            terminal.displayName,
                            terminal.identifier ?: "",
                        ),
                        style = WalleeTextStyles.label,
                        color = WalleeColors.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(
                    text = when {
                        !hasCamera -> stringResource(R.string.scan_no_camera)
                        granted -> stringResource(R.string.scan_placeholder_note)
                        denied -> stringResource(R.string.scan_permission_denied)
                        else -> stringResource(R.string.scan_rationale)
                    },
                    style = WalleeTextStyles.footnote,
                    color = WalleeColors.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                if (hasCamera && !granted) {
                    Spacer(Modifier.height(WalleeSpacing.S2))
                    if (denied) {
                        WTextButton(
                            text = stringResource(R.string.scan_open_settings),
                            onDark = true,
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            },
                        )
                    }
                    WSecondaryButton(text = stringResource(R.string.scan_permission_allow), onClick = {
                        launcher.launch(Manifest.permission.CAMERA)
                    }, onDark = true)
                }
                (flow as? LinkFlowState.Confirm)?.let { confirm ->
                    Text(
                        text = stringResource(R.string.scan_detected_announce, confirm.serial),
                        modifier = Modifier
                            .size(1.dp)
                            .semantics { liveRegion = LiveRegionMode.Assertive },
                        color = Color.Transparent,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WalleeSpacing.Side),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!credentials) {
                    WSecondaryButton(text = stringResource(R.string.scan_manual_entry), onClick = {
                        manualOpen = true
                    }, onDark = true, large = true)
                } else {
                    CredentialScanControls(
                        spacesOnly = spacesOnly,
                        onApplied = onCredentialsScanned,
                        externalRaw = credentialRaw,
                        onExternalConsumed = { credentialRaw = null },
                    )
                }
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }

        if (flow is LinkFlowState.Working) {
            ProgressOverlay(flow, mode)
        }
    }

    if (manualOpen) {
        WBottomSheet(onDismissRequest = { manualOpen = false }) {
            Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
                Text(text = stringResource(R.string.scan_manual_entry), style = WalleeTextStyles.sectionTitle)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = manualValue,
                    onValueChange = { manualValue = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' } },
                    label = stringResource(R.string.detail_label_serial),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (manualValue.length >=
                            6
                        ) {
                            submitManual(manualValue, linkViewModel) { manualOpen = false }
                        }
                    }),
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
                WPrimaryButton(
                    text = stringResource(R.string.action_continue),
                    onClick = { submitManual(manualValue, linkViewModel) { manualOpen = false } },
                    enabled = manualValue.length >= 6,
                    large = true,
                )
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }

    if (linkViewModel != null && terminal != null) {
        when (flow) {
            is LinkFlowState.Confirm ->
                ConfirmSheet(
                    confirm = flow,
                    name = terminal.displayName,
                    identifier = terminal.identifier ?: "",
                    mode = mode,
                    unlinkDone = ui.unlinkDone,
                    viewModel = linkViewModel,
                    onManual = { manualOpen = true },
                )

            is LinkFlowState.Invalid -> InvalidSheet(flow, linkViewModel, onManual = { manualOpen = true })

            is LinkFlowState.Failed -> FailedSheet(flow, linkViewModel, onToList = onToList, onClose = onClose)

            else -> Unit
        }
    }
}

private fun submitManual(value: String, viewModel: LinkFlowViewModel?, close: () -> Unit) {
    close()
    viewModel?.rescan()
    viewModel?.onScanned(value.trim())
}

/** Darkens everything except the viewfinder (black 60 %, docs/04 §Scanner-Overlay). */
@Composable
private fun Dim(left: Dp, top: Dp, finder: Size) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(WalleeColors.ScannerDim)
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left.toPx(), top.toPx()),
            size = Size(finder.width.dp.toPx(), finder.height.dp.toPx()),
            blendMode = BlendMode.Clear,
        )
    }
}

@Composable
private fun ProgressOverlay(working: LinkFlowState.Working, mode: ScanMode) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WalleeColors.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(WalleeSpacing.Side)
                .background(WalleeColors.Bg, CardShape)
                .padding(WalleeSpacing.S3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spinner()
            Spacer(Modifier.height(WalleeSpacing.S2))
            if (mode == ScanMode.REPLACE && working.totalSteps == 2) {
                StepRow(
                    1,
                    stringResource(R.string.link_step_unlink),
                    done = working.step > 1,
                    active =
                        working.step == 1,
                )
                StepRow(2, stringResource(R.string.link_step_link), done = false, active = working.step == 2)
            } else {
                Text(text = stringResource(R.string.link_working), style = WalleeTextStyles.bodyTitle)
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, label: String, done: Boolean, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$number",
            style = WalleeTextStyles.bodyTitle,
            color = if (active ||
                done
            ) {
                WalleeColors.Black
            } else {
                WalleeColors.TextMuted
            },
        )
        Spacer(Modifier.padding(WalleeSpacing.S1))
        Text(
            text = label,
            style = WalleeTextStyles.body,
            color = if (active ||
                done
            ) {
                WalleeColors.Text
            } else {
                WalleeColors.TextMuted
            },
        )
        Spacer(Modifier.padding(WalleeSpacing.S1))
        if (done) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = WalleeColors.TurquoiseText,
                modifier = Modifier.size(WalleeSize.IconSmall),
            )
        } else if (active) {
            Text(text = "…", style = WalleeTextStyles.body)
        }
    }
}

@Composable
private fun ConfirmSheet(
    confirm: LinkFlowState.Confirm,
    name: String,
    identifier: String,
    mode: ScanMode,
    unlinkDone: Boolean,
    viewModel: LinkFlowViewModel,
    onManual: () -> Unit,
) {
    WBottomSheet(onDismissRequest = viewModel::rescan) {
        Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
            Text(text = stringResource(R.string.scan_confirm_title), style = WalleeTextStyles.sectionTitle)
            Spacer(Modifier.height(WalleeSpacing.S2))
            Text(text = confirm.serial, style = WalleeTextStyles.serial)
            confirm.error?.let {
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(text = it, style = WalleeTextStyles.label, color = WalleeColors.Orange)
                Text(text = stringResource(R.string.scan_confirm_error_hint), style = WalleeTextStyles.footnote)
            }
            Spacer(Modifier.height(WalleeSpacing.S2))
            Text(
                text = stringResource(R.string.scan_confirm_link_with, name, identifier),
                style = WalleeTextStyles.body,
            )
            Spacer(Modifier.height(WalleeSpacing.S3))
            WPrimaryButton(
                text = stringResource(
                    if (mode == ScanMode.REPLACE &&
                        !unlinkDone
                    ) {
                        R.string.scan_confirm_replace
                    } else {
                        R.string.scan_confirm_link
                    },
                ),
                onClick = viewModel::confirm,
                large = true,
            )
            Spacer(Modifier.height(WalleeSpacing.S1))
            Row(horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1)) {
                WSecondaryButton(
                    text = stringResource(R.string.scan_rescan),
                    onClick = viewModel::rescan,
                    modifier = Modifier.weight(1f),
                )
                WSecondaryButton(
                    text = stringResource(R.string.scan_manual_short),
                    onClick = onManual,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(WalleeSpacing.S3))
        }
    }
}

@Composable
private fun InvalidSheet(invalid: LinkFlowState.Invalid, viewModel: LinkFlowViewModel, onManual: () -> Unit) {
    WBottomSheet(onDismissRequest = viewModel::rescan) {
        Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
            Text(text = stringResource(R.string.scan_invalid_title), style = WalleeTextStyles.sectionTitle)
            Spacer(Modifier.height(WalleeSpacing.S1))
            Text(text = stringResource(R.string.scan_invalid_text, invalid.raw), style = WalleeTextStyles.label)
            Spacer(Modifier.height(WalleeSpacing.S3))
            WPrimaryButton(text = stringResource(R.string.scan_rescan), onClick = viewModel::rescan, large = true)
            Spacer(Modifier.height(WalleeSpacing.S1))
            WSecondaryButton(text = stringResource(R.string.scan_manual_entry), onClick = onManual, large = true)
            Spacer(Modifier.height(WalleeSpacing.S3))
        }
    }
}

@Composable
private fun FailedSheet(
    failed: LinkFlowState.Failed,
    viewModel: LinkFlowViewModel,
    onToList: () -> Unit,
    onClose: () -> Unit,
) {
    WBottomSheet(onDismissRequest = {}) {
        Column(modifier = Modifier.padding(horizontal = WalleeSpacing.Side).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = WalleeColors.Orange,
                    modifier = Modifier.size(WalleeSize.Icon),
                )
                Spacer(Modifier.padding(WalleeSpacing.S1))
                Text(
                    text = stringResource(
                        when (failed.phase) {
                            FailedPhase.UNLINK_BEFORE_REPLACE -> R.string.link_error_unlink_title
                            FailedPhase.LINK_AFTER_UNLINK -> R.string.link_error_partial_title
                            FailedPhase.NETWORK_UNCLEAR -> R.string.error_network
                        },
                    ),
                    style = WalleeTextStyles.sectionTitle,
                )
            }
            Spacer(Modifier.height(WalleeSpacing.S1))
            Text(
                text = when (failed.phase) {
                    FailedPhase.UNLINK_BEFORE_REPLACE -> stringResource(R.string.link_error_unlink_text)
                    FailedPhase.LINK_AFTER_UNLINK -> stringResource(R.string.link_error_partial_text)
                    FailedPhase.NETWORK_UNCLEAR -> stringResource(R.string.link_network_unclear)
                },
                style = WalleeTextStyles.body,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = failed.message, style = WalleeTextStyles.label, color = WalleeColors.Orange)
            Spacer(Modifier.height(WalleeSpacing.S3))
            when (failed.phase) {
                FailedPhase.UNLINK_BEFORE_REPLACE -> {
                    WPrimaryButton(
                        text = stringResource(R.string.action_retry),
                        onClick = viewModel::relinkOrRetry,
                        large = true,
                    )
                    Spacer(Modifier.height(WalleeSpacing.S1))
                    WSecondaryButton(text = stringResource(R.string.action_cancel), onClick = onClose, large = true)
                }

                FailedPhase.LINK_AFTER_UNLINK -> {
                    WPrimaryButton(
                        text = stringResource(R.string.scan_rescan),
                        onClick = viewModel::retryLinkOnly,
                        large = true,
                    )
                    Spacer(Modifier.height(WalleeSpacing.S1))
                    if (failed.previousSerial != null) {
                        WTextButton(
                            text = stringResource(R.string.link_relink_old),
                            onClick = viewModel::relinkOld,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    WSecondaryButton(text = stringResource(R.string.link_to_list), onClick = onToList, large = true)
                }

                FailedPhase.NETWORK_UNCLEAR -> {
                    WPrimaryButton(
                        text = stringResource(R.string.link_check_state),
                        onClick = viewModel::checkState,
                        large = true,
                    )
                    Spacer(Modifier.height(WalleeSpacing.S1))
                    WSecondaryButton(text = stringResource(R.string.link_to_list), onClick = onToList, large = true)
                }
            }
            Spacer(Modifier.height(WalleeSpacing.S3))
        }
    }
}

/** The scanner is portrait only (docs/01) with light status-bar icons on the black camera view; both are restored on exit. */
@Composable
private fun LockPortrait() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val insets = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insets?.isAppearanceLightStatusBars = false
        onDispose {
            activity?.requestedOrientation = previous
            insets?.isAppearanceLightStatusBars = true
        }
    }
}

@Suppress("unused")
private val unusedOffset = IntOffset.Zero
