package com.zeroos.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.ui.ZeroTheme
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB")
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val time = ZonedDateTime.parse("2026-10-15T10:42:00+05:30[Asia/Kolkata]")
    private var swipes = 0
    private var longPresses = 0

    private fun show() = compose.setContent {
        ZeroTheme { HomeScreen(time = time, onSwipeUp = { swipes++ }, onLongPress = { longPresses++ }) }
    }

    @Test
    fun f1_1_f1_2_showsClockAndDate() {
        show()
        compose.onNodeWithText("10:42").assertExists()
        compose.onNodeWithText("Thursday 15 October").assertExists()
    }

    @Test
    fun f4_1_swipeUpOpensAppList() {
        show()
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeUp() }
        assertEquals(1, swipes)
    }

    @Test
    fun f4_1_swipeDownDoesNotOpenAppList() {
        show()
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeDown() }
        assertEquals(0, swipes)
    }

    @Test
    fun f1_9_longPressOnEmptySpaceOpensMenu() {
        show()
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { longClick(center) }
        assertEquals(1, longPresses)
        assertEquals(0, swipes)
    }

    @Test
    fun step1_3_homeScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }
}
