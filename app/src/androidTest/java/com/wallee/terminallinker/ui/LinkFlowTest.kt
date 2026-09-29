package com.wallee.terminallinker.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeTheme
import com.wallee.terminallinker.feature.scan.BarcodeScanner
import com.wallee.terminallinker.feature.scan.ScanScreen
import com.wallee.terminallinker.navigation.ResultOutcome
import com.wallee.terminallinker.navigation.ScanMode
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Link flow with a mocked scanner (docs/05 Phase 5.6): scan → confirm → link 204 → reload → Done. */
@RunWith(AndroidJUnit4::class)
class LinkFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val camera: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val wallee = MockWallee()
    private var linked = false
    private var closed = false
    private var result: List<Any?> = emptyList()

    /** Emits one value as soon as the screen is scanning; nothing while paused. */
    private val fakeScanner = BarcodeScanner { _, _, paused, _, onDetected, _ ->
        LaunchedEffect(paused) { if (!paused) onDetected("SN:2290012345") }
    }

    @Before
    fun setUp() {
        wallee.start()
        wallee.signIn()
        wallee.route("GET", "/payment/terminals/1003") {
            wallee.json(MockWallee.terminal(1003, "Kasse 3", serial = if (linked) "2290012345" else null))
        }
        wallee.route("POST", "/payment/terminals/1003/link") { request ->
            assertTrue(request.path!!.contains("serialNumber=2290012345"))
            linked = true
            MockResponse().setResponseCode(204)
        }
        compose.setContent {
            WalleeTheme {
                ScanScreen(
                    mode = ScanMode.LINK,
                    terminalId = 1003,
                    onClose = { closed = true },
                    onLinked = { id, outcome, serial, previous, confirmed ->
                        result = listOf(id, outcome, serial, previous, confirmed)
                    },
                    onCredentialsScanned = {},
                    onToList = {},
                    scanner = fakeScanner,
                )
            }
        }
    }

    @After
    fun tearDown() = wallee.stop()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    @Test
    fun scannedSerialIsConfirmedAndLinked() {
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("2290012345")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(string(R.string.scan_confirm_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_confirm_link_with, "Kasse 3", "WT-1003")).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_confirm_link)).performClick()
        compose.waitUntil(10_000) { result.isNotEmpty() }
        assertEquals(listOf<Any?>(1003L, ResultOutcome.LINKED, "2290012345", null, true), result)
        assertTrue(linked)
        assertEquals(false, closed)
    }
}
