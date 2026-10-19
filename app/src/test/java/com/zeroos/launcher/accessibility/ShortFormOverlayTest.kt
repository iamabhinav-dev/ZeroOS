package com.zeroos.launcher.accessibility

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.YOUTUBE_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class ShortFormOverlayTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val content = OverlayContent(
        overlay = Overlay.ShortForm(YOUTUBE_PACKAGE, allowLeft = 1, autoHomeSeconds = null),
        app = AppEntry(AppKey(YOUTUBE_PACKAGE, "Main", 0), "YouTube"),
        pun = 0,
        buttonPun = 0,
        appearance = AppearanceSettings(),
        pauseSeconds = 3,
        pausePun = 0
    )
    private val actions = OverlayActions(onGoHome = { calls += "home" }, onAllow = { calls += "allow" })

    @Test
    fun f16_11_allowGoesThroughTheMindfulPauseFirst() {
        compose.mainClock.autoAdvance = false
        compose.setContent { OverlayScreen(content, actions) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Allow 5 minutes (1)").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Breathe in. Is this really the plan?").assertExists()
        compose.onNodeWithText("Open YouTube").assertDoesNotExist()
        // "Go back" returns to the block screen without allowing anything.
        compose.onNodeWithText("Go back").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Allow 5 minutes (1)").performClick()
        compose.mainClock.advanceTimeBy(3_100)
        compose.onNodeWithText("Open YouTube").performClick()
        assertEquals(listOf("allow"), calls)
    }

    @Test
    fun f16_10_goHome() {
        compose.mainClock.autoAdvance = false
        compose.setContent { OverlayScreen(content, actions) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Go home").performClick()
        assertEquals(listOf("home"), calls)
    }
}
