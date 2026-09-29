package com.wallee.terminallinker.feature.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeSpacing
import com.wallee.terminallinker.core.ui.WalleeTextStyles
import com.wallee.terminallinker.core.ui.components.Headline
import com.wallee.terminallinker.core.ui.components.WBackButton
import com.wallee.terminallinker.core.ui.components.WHeader
import com.wallee.terminallinker.core.ui.components.WListRow
import com.wallee.terminallinker.core.ui.components.WScreen

private const val APACHE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
private const val MLKIT_TERMS_URL = "https://developers.google.com/ml-kit/terms"

private data class LicenseEntry(val nameRes: Int, val licenseRes: Int, val url: String)

private val entries = listOf(
    LicenseEntry(R.string.licenses_roboto, R.string.licenses_apache, APACHE_URL),
    LicenseEntry(R.string.licenses_mlkit, R.string.licenses_mlkit_terms, MLKIT_TERMS_URL),
    LicenseEntry(R.string.licenses_androidx, R.string.licenses_apache, APACHE_URL),
    LicenseEntry(R.string.licenses_okhttp, R.string.licenses_apache, APACHE_URL),
    LicenseEntry(R.string.licenses_kotlinx, R.string.licenses_apache, APACHE_URL),
)

/** Static license overview (docs/03 §Einstellungen → Über); the generated OSS list follows in phase 6. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    WScreen(header = { WHeader(leading = { WBackButton(onClick = onBack) }) }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(WalleeSpacing.S3))
            Headline(
                line1 = stringResource(R.string.space_settings),
                line2 = stringResource(R.string.licenses_title_2),
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S2))
            Text(
                text = stringResource(R.string.licenses_intro),
                style = WalleeTextStyles.body,
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S2))
            entries.forEach { entry ->
                WListRow(
                    title = stringResource(entry.nameRes),
                    subtitle = stringResource(entry.licenseRes),
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, entry.url.toUri())) }
                    },
                )
            }
            Spacer(Modifier.height(WalleeSpacing.S2))
            Text(
                text = stringResource(R.string.licenses_full_list_note),
                style = WalleeTextStyles.footnote,
                modifier = Modifier.padding(horizontal = WalleeSpacing.Side),
            )
            Spacer(Modifier.height(WalleeSpacing.S4))
        }
    }
}
