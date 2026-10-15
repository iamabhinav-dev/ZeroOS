package com.zeroos.launcher

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Sample Robolectric UI test and Roborazzi screenshot proving the test setup works (Step 0.6). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun step0_6_placeholderShowsAppName() {
        compose.onNodeWithText("ZeroOS").assertExists()
    }

    @Test
    fun step0_6_placeholderScreenshot() {
        compose.onRoot().captureRoboImage()
    }
}
