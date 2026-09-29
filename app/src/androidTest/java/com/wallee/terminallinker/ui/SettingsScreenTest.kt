package com.wallee.terminallinker.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.ui.WalleeTheme
import com.wallee.terminallinker.feature.settings.SettingsScreen
import com.wallee.terminallinker.feature.spaces.SpaceRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Settings: a discovered space can be removed (with confirmation) and restored; a manual one is deleted. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val wallee = MockWallee()

    @Before
    fun setUp() {
        wallee.start()
        wallee.signIn(spaceId = 2, spaceName = "Manual Space")
        runBlocking { wallee.container.uiPrefs.setDiscoveredSpaces(listOf(SpaceRef(1, "Hotel Muster"))) }
        compose.setContent {
            WalleeTheme {
                SettingsScreen(
                    onBack = {},
                    onEditCredentials = {},
                    onScanSpaces = {},
                    onOpenLicenses = {},
                    onWiped = {},
                    onOpenStyleguide = null,
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Hotel Muster")).fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun tearDown() = wallee.stop()

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun spaces() = runBlocking { wallee.container.spaceRepository.spaces.first().map { it.id } }

    @Test
    fun discoveredSpaceIsRemovedAfterConfirmationAndCanBeRestored() {
        // Rows are sorted by name: "Hotel Muster" (discovered) comes before "Manual Space".
        compose.onAllNodesWithText(string(R.string.space_remove))[0].performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.settings_remove_space_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.settings_remove_space_text, "Hotel Muster", "1")).assertIsDisplayed()
        compose.onAllNodesWithText(string(R.string.space_remove)).onLast().performClick()
        compose.waitUntil(5_000) { spaces() == listOf(2L) }

        compose.onNodeWithText(string(R.string.settings_removed_spaces)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.settings_space_restore)).performScrollTo().performClick()
        compose.waitUntil(5_000) { spaces() == listOf(1L, 2L) }
    }

    @Test
    fun manualSpaceIsDeletedAndActiveSpaceCleared() {
        compose.onAllNodesWithText(string(R.string.space_remove))[1].performScrollTo().performClick()
        compose.onAllNodesWithText(string(R.string.space_remove)).onLast().performClick()
        compose.waitUntil(5_000) { spaces() == listOf(1L) }
        assertEquals(null, runBlocking { wallee.container.spaceRepository.activeSpace.first() })
        compose.onNodeWithText(string(R.string.settings_removed_spaces)).assertDoesNotExist()
    }
}
