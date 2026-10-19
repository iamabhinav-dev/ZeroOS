package com.zeroos.launcher

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.feature.settings.SettingsPage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h300dp-xxhdpi")
class NotHomeAppBannerTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun show() = compose.setContent {
        ZeroTheme { NotHomeAppBanner(onSetDefault = { calls += "set" }, onDismiss = { calls += "dismiss" }) }
    }

    @Test
    fun f14_7_setAsDefaultAndDismiss() {
        show()
        compose.onNodeWithText("ZeroOS isn't your home app").assertExists()
        compose.onNodeWithText("Set as default").performClick()
        compose.onNodeWithText("Dismiss").performClick()
        assertEquals(listOf("set", "dismiss"), calls)
    }

    @Test
    fun f14_7_notHomeAppScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }
}

class FocusNavigationTest {
    @org.junit.Test
    fun d051_backReturnsWhereFocusOrInsightsWasOpened() {
        org.junit.Assert.assertEquals(LauncherScreen.SETTINGS to SettingsPage.FOCUS, backFrom(SettingsPage.FOCUS))
        org.junit.Assert.assertEquals(LauncherScreen.SETTINGS to SettingsPage.SCREEN_TIME, backFrom(SettingsPage.SCREEN_TIME))
        org.junit.Assert.assertEquals(LauncherScreen.HOME to SettingsPage.MAIN, backFrom(null))
    }
}
