package com.wallee.terminallinker.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeTheme
import com.wallee.terminallinker.feature.setup.SetupScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Setup (docs/05 Phase 5.6): wrong credentials show the 401 text, right ones find spaces and continue. */
@RunWith(AndroidJUnit4::class)
class SetupScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val wallee = MockWallee()
    private var continued = 0

    @Before
    fun setUp() {
        wallee.start()
    }

    @After
    fun tearDown() = wallee.stop()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun show() {
        compose.setContent {
            WalleeTheme { SetupScreen(onContinue = { continued++ }, onScanCredentials = {}) }
        }
    }

    private fun fillAndSubmit(userId: String) {
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput(userId)
        fields[1].performTextInput(MockWallee.TEST_KEY)
        // The keyboard would cover the button at the bottom of the form.
        MockWallee.hideKeyboard(compose.activity)
        compose.onNodeWithText(string(R.string.setup_test_and_save)).performScrollTo().performClick()
    }

    @Test
    fun wrongCredentialsShowPlainTextErrorAndStay() {
        wallee.route("GET", "/spaces") { wallee.json("""{"message":"Unauthorized"}""", status = 401) }
        show()
        fillAndSubmit("999")
        compose.waitUntil(10_000) {
            compose.onAllNodes(
                androidx.compose.ui.test.hasText(string(R.string.error_401)),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        // The message sits under the button; on small screens it may be composed but below the fold.
        compose.onNodeWithText(string(R.string.error_401)).assertExists()
        assertEquals(0, continued)
        assertNull(wallee.container.credentialStore.load())
    }

    @Test
    fun rightCredentialsFindSpacesAndContinue() {
        wallee.route("GET", "/spaces") {
            wallee.json("""{"data":[{"id":67890,"name":"Hotel Muster","state":"ACTIVE"}],"hasMore":false}""")
        }
        show()
        fillAndSubmit("12345")
        compose.waitUntil(10_000) { continued > 0 }
        assertEquals(12345L, wallee.container.credentialStore.load()?.userId)
        val request = wallee.server.takeRequest()
        assertEquals("/api/v2.0/spaces?limit=10", request.path)
    }
}
