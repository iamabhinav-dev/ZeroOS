package com.zeroos.launcher.safemode

import android.content.ComponentName
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SafeModeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val apps = listOf("Calculator", "Calendar", "Camera", "Chrome")
        .map { SafeModeApp(it, ComponentName("pkg.${it.lowercase()}", "Main"), Process.myUserHandle()) }
    private val calls = mutableListOf<String>()

    private fun show() = compose.setContent {
        ZeroTheme {
            SafeModeScreen(
                apps = apps,
                onAppClick = { calls += it.label },
                onViewReport = { calls += "report" },
                onHomeSettings = { calls += "home" },
                onExit = { calls += "exit" }
            )
        }
    }

    @Test
    fun f18_3_showsBannerActionsAndApps() {
        show()
        listOf("Safe mode", "ZeroOS crashed repeatedly.", "View crash report", "Open Home settings", "Exit safe mode", "Camera")
            .forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun f18_3_actionsAndAppTapsCallBack() {
        show()
        listOf("View crash report", "Open Home settings", "Exit safe mode", "Camera").forEach { compose.onNodeWithText(it).performClick() }
        assertEquals(listOf("report", "home", "exit", "Camera"), calls)
    }

    @Test
    fun step1_5_safeModeScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_safeModeScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }
}
