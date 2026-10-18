package com.zeroos.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HoldToConfirmTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var confirmed = 0

    private fun show() = compose.setContent { ZeroTheme { HoldToConfirm("Hold to turn off", 3.seconds, { confirmed++ }) } }

    @Test
    fun f8_12_holdingForTheFullTimeConfirms() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Hold to turn off").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(3_100)
        assertEquals(1, confirmed)
        compose.onNodeWithText("Hold to turn off").performTouchInput { up() }
    }

    @Test
    fun f8_12_lettingGoEarlyDoesNothing() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Hold to turn off").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Hold to turn off").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(0, confirmed)
    }
}
