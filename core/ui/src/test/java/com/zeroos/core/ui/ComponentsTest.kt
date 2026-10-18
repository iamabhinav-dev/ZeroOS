package com.zeroos.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private companion object {
        const val PHONE = "w392dp-h872dp-xxhdpi"
    }

    @Test
    fun nfrA2_listItemsAreAtLeast48dpAndClickable() {
        compose.setContent {
            ZeroTheme { MinimalListItem("Camera", onClick = { calls += "click" }, onLongClick = { calls += "long" }) }
        }
        compose.onNodeWithText("Camera").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Camera").performClick()
        compose.onNodeWithText("Camera").performTouchInput { longClick() }
        assertEquals(listOf("click", "long"), calls)
    }

    @Test
    fun nfrA1_toggleRowIsAnnouncedAsSwitchWithState() {
        compose.setContent {
            var on by remember { mutableStateOf(false) }
            ZeroTheme { ToggleRow("Show battery %", on, { on = it }) }
        }
        val node = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "off"))
        node.performClick()
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "on"))
    }

    @Test
    fun step2_2_settingRowAndActionsCallBack() {
        compose.setContent {
            ZeroTheme {
                Column {
                    SettingRow("Theme", "Dark", { calls += "theme" })
                    TextAction("Export logs", { calls += "export" })
                }
            }
        }
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Export logs").performClick()
        assertEquals(listOf("theme", "export"), calls)
    }

    @Composable
    private fun Gallery() {
        val tokens = ZeroTheme.tokens
        Column(Modifier.background(tokens.colors.background)) {
            ScreenTitle("Settings")
            AppLabel("WhatsApp")
            MinimalListItem("Appearance", onClick = {}, trailing = "›")
            SettingRow("Text size", "Medium", onClick = {})
            ToggleRow("Show battery %", checked = true, onCheckedChange = {})
            BodyText("Hidden apps are not shown in the app list or search.", muted = true)
            TextAction("Export logs", onClick = {})
        }
    }

    /** Rendered at a common phone density (about 1080 x 2400, xxhdpi) so Doto's dots are visible. */
    @Test
    @Config(qualifiers = PHONE)
    fun step2_2_galleryDarkScreenshot() {
        compose.setContent { ZeroTheme(ThemeMode.DARK) { Gallery() } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun step2_2_galleryLightScreenshot() {
        compose.setContent { ZeroTheme(ThemeMode.LIGHT) { Gallery() } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = PHONE, fontScale = 1.3f)
    fun step2_2_galleryLargestTextScreenshot() {
        compose.setContent { ZeroTheme(ThemeMode.DARK, TextSize.EXTRA_LARGE) { Gallery() } }
        compose.onRoot().captureRoboImage()
    }
}
