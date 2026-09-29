package com.wallee.terminallinker.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeTheme
import com.wallee.terminallinker.feature.terminals.TerminalsScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Terminal list (docs/05 Phase 5.6): loads 100 terminals from the mock, search and filter react locally. */
@RunWith(AndroidJUnit4::class)
class TerminalsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val wallee = MockWallee()
    private val opened = mutableListOf<Long>()
    private val quickLinked = mutableListOf<Long>()

    @Before
    fun setUp() {
        wallee.start()
        wallee.signIn()
        val page = wallee.asset("mock-terminals-page.json")
        wallee.route("GET", "/payment/terminals/search") { request ->
            // The fixture says hasMore=true; the second page (offset 100) is empty and ends the loading.
            if ((request.path ?: "").contains("offset=0") || !(request.path ?: "").contains("offset=")) {
                wallee.json(page)
            } else {
                wallee.json("""{"data":[],"hasMore":false}""")
            }
        }
        compose.setContent {
            WalleeTheme {
                TerminalsScreen(
                    onOpenTerminal = { opened += it },
                    onQuickLink = { quickLinked += it },
                    onOpenSettings = {},
                    onScanSpaces = {},
                )
            }
        }
        // Only visible rows exist in the tree (LazyColumn), so wait for any row rather than a specific one.
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Kasse ", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() = wallee.stop()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    /** A list row by title — excludes the search field, whose editable text also matches while typing. */
    private fun row(title: String): SemanticsNodeInteraction = compose.onNode(hasText(title) and !hasSetTextAction())

    /** A filter segment by label — excludes the identically named badges on the rows. */
    private fun segment(label: String): SemanticsNodeInteraction =
        compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    @Test
    fun searchFiltersImmediately() {
        compose.onNode(hasSetTextAction()).performTextInput("Kasse 10")
        // Existence, not visibility: rows beyond the fold are composed but not displayed on small screens.
        row("Kasse 10").assertExists()
        row("Kasse 100").assertExists()
        row("Kasse 1").assertDoesNotExist()
        row("Kasse 2").assertDoesNotExist()
    }

    @Test
    fun unlinkedFilterShowsOnlyUnlinkedAndTapOpensDetail() {
        // Narrow with the search first so every matching row fits on screen: Kasse 3, 30–39.
        compose.onNode(hasSetTextAction()).performTextInput("Kasse 3")
        Espresso.closeSoftKeyboard()
        row("Kasse 3").assertExists()
        segment(string(R.string.terminals_filter_unlinked)).performClick()
        row("Kasse 3").assertIsDisplayed()
        row("Kasse 30").assertExists()
        row("Kasse 32").assertDoesNotExist()
        row("Kasse 31").assertDoesNotExist()
        row("Kasse 3").performClick()
        assertEquals(listOf(1003L), opened)
    }
}
