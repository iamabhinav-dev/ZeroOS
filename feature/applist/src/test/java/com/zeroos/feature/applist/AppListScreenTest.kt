package com.zeroos.feature.applist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppListScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val apps = listOf("Calculator", "Calendar", "Camera", "Chrome", "Clock", "Contacts", "Drive", "Gmail")
        .map { AppEntry(AppKey("pkg.${it.lowercase()}", "Main", 0), it, isMainProfile = true) }
    private val clicked = mutableListOf<AppEntry>()
    private var closed = 0

    private fun show() = compose.setContent {
        ZeroTheme { AppListScreen(apps = apps, onAppClick = { clicked += it }, onClose = { closed++ }) }
    }

    @Test
    fun f2_2_showsAppsAsTextList() {
        show()
        apps.forEach { compose.onNodeWithText(it.label).assertExists() }
    }

    @Test
    fun step1_3_tapLaunchesApp() {
        show()
        compose.onNodeWithText("Camera").performClick()
        assertEquals(listOf("Camera"), clicked.map { it.label })
    }

    @Test
    fun f2_11_backClosesList() {
        show()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(1, closed)
    }

    @Test
    fun step1_3_appListScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }
}
