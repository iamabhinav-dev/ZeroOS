package com.zeroos.feature.wellbeing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.settings.ThemeMode
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
class ShortFormScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun show(message: String? = null, allowLeft: Int = 1, autoHome: Int? = null, theme: ThemeMode = ThemeMode.DARK) =
        compose.setContent {
            ZeroTheme(theme) {
                ShortFormScreen(19, message, allowLeft, autoHome, onGoHome = { calls += "home" }, onAllow = {
                    calls +=
                        "allow"
                })
            }
        }

    @Test
    fun f16_7_f16_9_punGoHomeAndAllow() {
        show()
        compose.onNodeWithText("Reels can wait. Your life can't.").assertExists()
        compose.onNodeWithContentDescription("Short video blocked").assertExists()
        compose.onNodeWithText("Allow 5 minutes (1)").performClick()
        compose.onNodeWithText("Go home").performClick()
        assertEquals(listOf("allow", "home"), calls)
    }

    @Test
    fun f16_9_customMessageInsteadOfAPun() {
        show(message = "Not now, future me.")
        compose.onNodeWithText("Not now, future me.").assertExists()
    }

    @Test
    fun f16_11_f16_16_noAllowWhenUsedUpOrFocusing() {
        show(allowLeft = 0)
        compose.onNodeWithText("Allow 5 minutes", substring = true).assertDoesNotExist()
    }

    @Test
    fun f16_10_goesHomeByItself() {
        compose.mainClock.autoAdvance = false
        show(autoHome = 3)
        compose.mainClock.advanceTimeBy(2_900)
        assertEquals(emptyList<String>(), calls)
        compose.mainClock.advanceTimeBy(200)
        assertEquals(listOf("home"), calls)
    }

    @Test
    fun step6_3_shortFormScreenshot() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step6_3_shortFormLightScreenshot() {
        compose.mainClock.autoAdvance = false
        show(theme = ThemeMode.LIGHT)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step6_3_shutterHalfwayLightScreenshot() {
        compose.mainClock.autoAdvance = false
        show(theme = ThemeMode.LIGHT)
        compose.mainClock.advanceTimeBy(450)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_shortFormScreenshot() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage()
    }
}
