package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class HiddenAppsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val apps = listOf("Instagram", "YouTube").map { AppEntry(AppKey("pkg.$it", "Main", 0), it) }

    @Test
    fun f5_2_listsHiddenAppsAndUnhides() {
        compose.setContent { ZeroTheme { HiddenAppsScreen(apps, onUnhide = { calls += it.label }, onBack = {}) } }
        compose.onNodeWithText("Hidden apps are not shown in the app list or search.").assertExists()
        compose.onAllNodes(androidx.compose.ui.test.hasText("Unhide"))[1].performClick()
        assertEquals(listOf("YouTube"), calls)
    }

    @Test
    fun step2_6_hiddenAppsScreenshot() {
        compose.setContent { ZeroTheme { HiddenAppsScreen(apps, onUnhide = {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f5_3_confirmGate() {
        compose.setContent {
            ZeroTheme {
                HiddenAppsGate(HiddenAppsConfirmation.CONFIRM, onOpen = { calls += "open" }, onCancel = {
                    calls +=
                        "cancel"
                })
            }
        }
        compose.onNodeWithText("Show hidden apps?").assertExists()
        compose.onNodeWithText("Show").performClick()
        assertEquals(listOf("open"), calls)
    }

    @Test
    fun f5_3_delayGateCountsDownFiveSeconds() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ZeroTheme {
                HiddenAppsGate(HiddenAppsConfirmation.DELAY, onOpen = { calls += "open" }, onCancel = {
                    calls +=
                        "cancel"
                })
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Opening in 5…").assertExists()
        compose.mainClock.advanceTimeBy(4_000)
        assertEquals(emptyList<String>(), calls)
        compose.mainClock.advanceTimeBy(1_100)
        assertEquals(listOf("open"), calls)
    }

    @Test
    fun f5_3_delayGateCanBeCancelled() {
        compose.setContent {
            ZeroTheme {
                HiddenAppsGate(HiddenAppsConfirmation.DELAY, onOpen = { calls += "open" }, onCancel = {
                    calls +=
                        "cancel"
                })
            }
        }
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("cancel", calls.first())
    }
}
