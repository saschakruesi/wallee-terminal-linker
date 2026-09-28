package com.wallee.terminallinker.feature.styleguide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.CardShape
import com.wallee.terminallinker.core.ui.Contrast
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeSize
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Hairline
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.LogoVariant
import com.wallee.terminallinker.core.ui.components.SpaceChip
import com.wallee.terminallinker.core.ui.components.Spinner
import com.wallee.terminallinker.core.ui.components.TurquoisePanel
import com.wallee.terminallinker.core.ui.components.ViewfinderCorners
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WBadge
import com.wallee.terminallinker.core.ui.components.WBadgeKind
import com.wallee.terminallinker.core.ui.components.WBottomSheet
import com.wallee.terminallinker.core.ui.components.WConfirmDialog
import com.wallee.terminallinker.core.ui.components.WFactRow
import com.wallee.terminallinker.core.ui.components.WFactTable
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WIconButton
import com.wallee.terminallinker.core.ui.components.WInput
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WPrimaryButton
import com.wallee.terminallinker.core.ui.components.WScreen
import com.wallee.terminallinker.core.ui.components.WSecondaryButton
import com.wallee.terminallinker.core.ui.components.WSegment
import com.wallee.terminallinker.core.ui.components.WSegmented
import com.wallee.terminallinker.core.ui.components.WTextButton
import com.wallee.terminallinker.core.ui.components.WToast
import com.wallee.terminallinker.core.ui.components.WToastKind
import com.wallee.terminallinker.core.ui.components.WalleeLogo
import com.wallee.terminallinker.core.ui.components.showToast
import kotlinx.coroutines.launch

/**
 * Debug-only reference of every component in every state (docs/04 §Styleguide-Screen). This is the
 * acceptance reference for phase 1: no purple, no shadows, Roboto everywhere, wordmark top right.
 */
@Composable
fun StyleguideScreen(onBack: () -> Unit) {
    val toasts = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var segment by rememberSaveable { mutableIntStateOf(0) }
    var inputEmpty by rememberSaveable { mutableStateOf("") }
    var inputFilled by rememberSaveable { mutableStateOf("12345") }
    var inputPassword by rememberSaveable { mutableStateOf("Q2xhdWRlVGVzdEtleQ==") }
    var inputError by rememberSaveable { mutableStateOf("99999") }
    val successText = stringResource(R.string.toast_placeholder_success)
    val errorText = stringResource(R.string.toast_placeholder_error)

    WScreen(
        header = { WHeader(leading = { WBackButton(onClick = onBack) }) },
        toastHost = toasts,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = WalleeSpacing.S5),
        ) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            Headline(
                line1 = stringResource(R.string.styleguide_title_1),
                line2 = stringResource(R.string.styleguide_title_2),
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )

            Section(stringResource(R.string.styleguide_section_colors)) {
                ColorGrid()
            }

            Section(stringResource(R.string.styleguide_section_type)) {
                Text("Terminals", style = WalleeTextStyles.headline1)
                Text("Hotel Muster – Rezeption", style = WalleeTextStyles.headline2)
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(stringResource(R.string.styleguide_sample_section), style = WalleeTextStyles.sectionTitle)
                Text(stringResource(R.string.styleguide_sample_body), style = WalleeTextStyles.body)
                Text("Listenzeile Titel 15 sp Medium", style = WalleeTextStyles.bodyTitle)
                Text(stringResource(R.string.styleguide_sample_label), style = WalleeTextStyles.label)
                Text(stringResource(R.string.styleguide_sample_footnote), style = WalleeTextStyles.footnote)
                Spacer(Modifier.height(WalleeSpacing.S2))
                Text(stringResource(R.string.styleguide_sample_serial), style = WalleeTextStyles.serial)
                Text("1234567890 · tnum", style = WalleeTextStyles.value)
            }

            Section(stringResource(R.string.styleguide_section_buttons)) {
                StateLabel(stringResource(R.string.styleguide_state_default))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WPrimaryButton(text = "Primär", onClick = {})
                    WSecondaryButton(text = "Sekundär", onClick = {})
                    WTextButton(text = "Text", onClick = {})
                    WTextButton(text = "Trennen", onClick = {}, destructive = true)
                }
                StateLabel(stringResource(R.string.styleguide_state_disabled))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WPrimaryButton(text = "Primär", onClick = {}, enabled = false)
                    WSecondaryButton(text = "Sekundär", onClick = {}, enabled = false)
                    WTextButton(text = "Text", onClick = {}, enabled = false)
                }
                StateLabel(stringResource(R.string.styleguide_state_loading))
                WPrimaryButton(text = "Verbindung testen & speichern", onClick = {}, loading = true, large = true)
                StateLabel("Gross, volle Breite / Icon")
                WPrimaryButton(text = "Gerät linken", onClick = {}, large = true)
                Spacer(Modifier.height(WalleeSpacing.S1))
                WSecondaryButton(text = "QR scannen", onClick = {}, iconRes = R.drawable.ic_qr_scan)
                StateLabel(stringResource(R.string.styleguide_state_on_turquoise))
                TurquoisePanel(contentPadding = WalleeSpacing.S2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WPrimaryButton(text = "Zurück zur Liste", onClick = {}, onTurquoise = true)
                        Spinner(onTurquoise = true)
                    }
                }
                StateLabel(stringResource(R.string.styleguide_state_on_dark))
                Box(modifier = Modifier.fillMaxWidth().background(WalleeColors.Black).padding(WalleeSpacing.S2)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WSecondaryButton(text = "Manuell eingeben", onClick = {}, onDark = true)
                        WTextButton(text = "Text", onClick = {}, onDark = true)
                        WIconButton(
                            iconRes = R.drawable.ic_flashlight,
                            contentDescription = stringResource(
                                R.string.cd_torch,
                            ),
                            onClick = {
                            },
                            tint = WalleeColors.White,
                        )
                    }
                }
                StateLabel("Icon-Button / Spinner")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WIconButton(iconRes = R.drawable.ic_search, contentDescription = "Suche", onClick = {})
                    WIconButton(
                        iconRes = R.drawable.ic_close,
                        contentDescription = stringResource(
                            R.string.action_close,
                        ),
                        onClick = {
                        },
                    )
                    Spinner()
                }
            }

            Section(stringResource(R.string.styleguide_section_inputs)) {
                WInput(value = inputEmpty, onValueChange = {
                    inputEmpty = it
                }, label = "Application User ID", placeholder = stringResource(R.string.styleguide_input_placeholder))
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(value = inputFilled, onValueChange = {
                    inputFilled = it
                }, label = stringResource(R.string.styleguide_state_default))
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(value = inputPassword, onValueChange = {
                    inputPassword = it
                }, label = "Authentication Key", password = true)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = inputError,
                    onValueChange = {
                        inputError = it
                    },
                    label = stringResource(
                        R.string.styleguide_state_error,
                    ),
                    error = stringResource(R.string.styleguide_input_error_text),
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(
                    value = "",
                    onValueChange = {
                    },
                    placeholder = stringResource(
                        R.string.terminals_search_hint,
                    ),
                    leadingIconRes = R.drawable.ic_search,
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                WInput(value = "12345", onValueChange = {
                }, label = stringResource(R.string.styleguide_state_disabled), enabled = false)
                Spacer(Modifier.height(WalleeSpacing.S1))
                Text(
                    stringResource(R.string.styleguide_state_focus) + ": Feld antippen → 2 dp Türkis",
                    style = WalleeTextStyles.footnote,
                )
            }

            Section(stringResource(R.string.styleguide_section_segmented), sidePadding = false) {
                WSegmented(
                    segments = listOf(WSegment("Alle", 12), WSegment("Ungelinkt", 3), WSegment("Gelinkt", 9)),
                    selectedIndex = segment,
                    onSelect = { segment = it },
                )
            }

            Section(stringResource(R.string.styleguide_section_badges)) {
                Row(horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1)) {
                    WBadgeKind.entries.forEach { WBadge(kind = it) }
                }
            }

            Section(stringResource(R.string.styleguide_section_rows), sidePadding = false) {
                WListRow(title = "Kasse 1", subtitle = "WT-8F3K2 · PAX A77 · Filiale Winterthur", badge = {
                    WBadge(WBadgeKind.Unlinked)
                }, onClick = {})
                WListRow(title = "Kasse 2", subtitle = "WT-8F3K3 · S/N 2290012345 · Filiale Winterthur", badge = {
                    WBadge(WBadgeKind.Linked)
                }, onClick = {})
                WListRow(title = "Bar Terminal", subtitle = "WT-8F3K9 · Filiale Zürich", badge = {
                    WBadge(WBadgeKind.Inactive)
                }, onClick = {})
                WListRow(title = "Ohne Chevron, nicht tippbar", subtitle = "Zweite Zeile", onClick = null)
                WListRow(
                    title = "Mit Leading-Icon",
                    onClick = {},
                    showChevron = false,
                    leading = {
                        Icon(
                            painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            tint = WalleeColors.Black,
                            modifier = Modifier.size(WalleeSize.Icon),
                        )
                    },
                )
            }

            Section(stringResource(R.string.styleguide_section_facts)) {
                WFactRow(label = "Status") { WBadge(WBadgeKind.Linked) }
                WFactTable(
                    rows = listOf(
                        "Gerät" to "PAX A77",
                        "Seriennummer" to "2290012345",
                        "Standort" to "Filiale Winterthur",
                        "Aktiviert am" to "12.03.2026",
                        "Terminal-ID (intern)" to "1002",
                    ),
                )
            }

            Section(stringResource(R.string.styleguide_section_header), sidePadding = false) {
                WHeader(leading = {
                    SpaceChip(name = "Hotel Muster – Rezeption", onClick = {}, modifier = Modifier.width(200.dp))
                })
                Spacer(Modifier.height(WalleeSpacing.S1))
                WHeader(leading = { WBackButton(onClick = {}) })
                Spacer(Modifier.height(WalleeSpacing.S2))
                Row(
                    modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
                    horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1),
                ) {
                    SpaceChip(name = "Kurz", onClick = {})
                    SpaceChip(name = "Sehr langer Space-Name der abgeschnitten wird", onClick = {
                    }, modifier = Modifier.width(180.dp))
                }
            }

            Section(stringResource(R.string.styleguide_section_overlays)) {
                Row(horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1)) {
                    WSecondaryButton(text = stringResource(R.string.styleguide_open_sheet), onClick = {
                        sheetOpen = true
                    })
                    WSecondaryButton(text = stringResource(R.string.styleguide_open_dialog), onClick = {
                        dialogOpen =
                            true
                    })
                }
                Spacer(Modifier.height(WalleeSpacing.S1))
                Row(horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1)) {
                    WSecondaryButton(text = stringResource(R.string.styleguide_show_toast_success), onClick = {
                        scope.launch { toasts.showToast(successText, WToastKind.Success) }
                    })
                    WSecondaryButton(text = stringResource(R.string.styleguide_show_toast_error), onClick = {
                        scope.launch { toasts.showToast(errorText, WToastKind.Error) }
                    })
                }
                Spacer(Modifier.height(WalleeSpacing.S2))
                Column(
                    modifier = Modifier.fillMaxWidth().border(
                        WalleeSize.Hairline,
                        WalleeColors.Grid,
                        CardShape,
                    ).padding(vertical = WalleeSpacing.S1),
                ) {
                    WToast(message = successText, kind = WToastKind.Success)
                    WToast(message = errorText, kind = WToastKind.Error)
                }
            }

            Section(stringResource(R.string.styleguide_section_panel), sidePadding = false) {
                TurquoisePanel(showLogo = true) {
                    Icon(
                        painterResource(R.drawable.ic_check_large),
                        contentDescription = null,
                        tint = WalleeColors.Black,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(WalleeSpacing.S2))
                    Text(stringResource(R.string.styleguide_sample_statement), style = WalleeTextStyles.statement)
                    Text("Kasse 1 · WT-8F3K2", style = WalleeTextStyles.body, color = WalleeColors.Black)
                }
                Spacer(Modifier.height(WalleeSpacing.S2))
                Box(
                    modifier = Modifier.fillMaxWidth().background(WalleeColors.Black).padding(WalleeSpacing.S3),
                    contentAlignment = Alignment.Center,
                ) {
                    ViewfinderCorners(width = 200.dp, height = 114.dp)
                }
            }

            Section(stringResource(R.string.styleguide_section_icons)) {
                IconGrid()
            }

            Section(stringResource(R.string.styleguide_section_logo)) {
                WalleeLogo(variant = LogoVariant.Turquoise, height = WalleeSize.LogoHeader)
                Spacer(Modifier.height(WalleeSpacing.S2))
                WalleeLogo(variant = LogoVariant.Black, height = WalleeSize.LogoHeader)
                Spacer(Modifier.height(WalleeSpacing.S2))
                Box(modifier = Modifier.background(WalleeColors.Turquoise).padding(22.dp)) {
                    WalleeLogo(variant = LogoVariant.White, height = WalleeSize.LogoPanel)
                }
            }
        }
    }

    if (sheetOpen) {
        WBottomSheet(onDismissRequest = { sheetOpen = false }) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Text(
                    stringResource(R.string.space_choose),
                    style = WalleeTextStyles.sectionTitle,
                    modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
                )
                Spacer(Modifier.height(WalleeSpacing.S2))
                WListRow(title = "Hotel Muster – Rezeption", subtitle = "12345", onClick = {
                    sheetOpen = false
                }, showChevron = false)
                WListRow(title = "Café Seeblick", subtitle = "23456", onClick = {
                    sheetOpen = false
                }, showChevron = false)
                Spacer(Modifier.height(WalleeSpacing.S3))
            }
        }
    }
    if (dialogOpen) {
        WConfirmDialog(
            title = stringResource(R.string.styleguide_dialog_title),
            text = stringResource(R.string.styleguide_dialog_text),
            confirmText = stringResource(R.string.styleguide_dialog_confirm),
            destructive = true,
            onConfirm = { dialogOpen = false },
            onDismiss = { dialogOpen = false },
        )
    }
}

@Composable
private fun Section(title: String, sidePadding: Boolean = true, content: @Composable () -> Unit) {
    Spacer(Modifier.height(WalleeSpacing.S4))
    Text(
        text = title,
        style = WalleeTextStyles.sectionTitle,
        modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
    )
    Spacer(Modifier.height(WalleeSpacing.S1))
    Hairline(modifier = Modifier.padding(horizontal = WalleeSpacing.Side))
    Spacer(Modifier.height(WalleeSpacing.S2))
    Column(modifier = if (sidePadding) Modifier.padding(horizontal = WalleeSpacing.Side) else Modifier) {
        content()
    }
}

@Composable
private fun StateLabel(text: String) {
    Spacer(Modifier.height(WalleeSpacing.S2))
    Text(text = text, style = WalleeTextStyles.label)
    Spacer(Modifier.height(WalleeSpacing.S1))
}

private val colorTokens = listOf(
    "Turquoise" to WalleeColors.Turquoise,
    "Turquoise80" to WalleeColors.Turquoise80,
    "Turquoise40" to WalleeColors.Turquoise40,
    "Turquoise20" to WalleeColors.Turquoise20,
    "TurquoiseText" to WalleeColors.TurquoiseText,
    "TurquoiseDeep" to WalleeColors.TurquoiseDeep,
    "Orange" to WalleeColors.Orange,
    "OrangeSoft" to WalleeColors.OrangeSoft,
    "OrangeText" to WalleeColors.OrangeText,
    "Black" to WalleeColors.Black,
    "Text" to WalleeColors.Text,
    "TextMuted" to WalleeColors.TextMuted,
    "Line" to WalleeColors.Line,
    "Grid" to WalleeColors.Grid,
    "BgSoft" to WalleeColors.BgSoft,
    "BadgeNeutral" to WalleeColors.BadgeNeutral,
)

@Composable
private fun ColorGrid() {
    colorTokens.chunked(4).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1)) {
            row.forEach { (name, color) ->
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(color, CardShape)
                            .border(WalleeSize.Hairline, WalleeColors.Grid, CardShape),
                    )
                    Text(text = name, style = WalleeTextStyles.footnote, maxLines = 1)
                    Text(
                        text = hex(color) + " · " + "%.1f".format(Contrast.ratio(color, WalleeColors.Bg)) + ":1",
                        style = WalleeTextStyles.footnote,
                        maxLines = 1,
                    )
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(WalleeSpacing.S1))
    }
}

private fun hex(color: Color): String {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

private val icons = listOf(
    "search" to R.drawable.ic_search,
    "close" to R.drawable.ic_close,
    "back" to R.drawable.ic_back,
    "chevron_right" to R.drawable.ic_chevron_right,
    "chevron_down" to R.drawable.ic_chevron_down,
    "check" to R.drawable.ic_check,
    "warning" to R.drawable.ic_warning,
    "flashlight" to R.drawable.ic_flashlight,
    "edit" to R.drawable.ic_edit,
    "copy" to R.drawable.ic_copy,
    "refresh" to R.drawable.ic_refresh,
    "settings" to R.drawable.ic_settings,
    "plus" to R.drawable.ic_plus,
    "eye" to R.drawable.ic_eye,
    "eye_off" to R.drawable.ic_eye_off,
    "camera" to R.drawable.ic_camera,
    "qr_scan" to R.drawable.ic_qr_scan,
    "more_vert" to R.drawable.ic_more_vert,
)

@Composable
private fun IconGrid() {
    icons.chunked(6).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WalleeSpacing.S1)) {
            row.forEach { (name, res) ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(res),
                        contentDescription = name,
                        tint = WalleeColors.Black,
                        modifier = Modifier.size(WalleeSize.Icon),
                    )
                    Text(
                        text = name,
                        style = WalleeTextStyles.footnote,
                        maxLines = 1,
                        modifier = Modifier.wrapContentWidth(),
                    )
                }
            }
            repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(WalleeSpacing.S2))
    }
}
