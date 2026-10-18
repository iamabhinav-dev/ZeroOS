package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.GestureSettings
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.TextSize
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
class SettingsScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun showMain(theme: ThemeMode = ThemeMode.DARK) = compose.setContent {
        ZeroTheme(theme) { SettingsMainScreen(onOpen = { calls += it.name }, onOpenDiagnostics = { calls += "diagnostics" }, onBack = {}) }
    }

    private fun showHome(state: HomeSettings = HomeSettings()) = compose.setContent {
        ZeroTheme {
            HomeSettingsScreen(
                state,
                HomeSettingsActions(
                    onEditFavorites = { calls += "favorites" },
                    onMaxFavorites = { calls += "max" },
                    onAlignment = { calls += "alignment" },
                    onShowBattery = { calls += "battery:$it" },
                    onShowNextAlarm = { calls += "alarm:$it" }
                ),
                onBack = {}
            )
        }
    }

    private fun showAppList(state: AppListSettings = AppListSettings(), textSize: TextSize = TextSize.MEDIUM) = compose.setContent {
        ZeroTheme(textSize = textSize) {
            AppListSettingsScreen(
                state,
                hiddenCount = 2,
                actions = AppListSettingsActions(
                    onKeyboardAutoOpen = { calls += "keyboard:$it" },
                    onAutoLaunch = { calls += "auto:$it" },
                    onFastScroller = { calls += "scroller:$it" },
                    onHiddenApps = { calls += "hidden" },
                    onConfirmation = { calls += "confirmation" }
                ),
                onBack = {}
            )
        }
    }

    private fun showGestures(state: SettingsUiState = SettingsUiState()) = compose.setContent {
        ZeroTheme {
            GesturesScreen(
                state,
                GesturesActions(
                    onSwipeLeft = { calls += "left" },
                    onSwipeRight = { calls += "right" },
                    onDoubleTapLock = { calls += "doubleTap:$it" },
                    onTurnOnAccessibility = { calls += "turnOn" }
                ),
                onBack = {}
            )
        }
    }

    @Test
    fun step2_11_mainOpensEverySection() {
        showMain()
        listOf("Home", "App list", "Gestures", "Appearance", "Wellbeing", "Backup", "Diagnostics", "About").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(listOf("HOME", "APP_LIST", "GESTURES", "APPEARANCE", "WELLBEING", "BACKUP", "diagnostics", "ABOUT"), calls)
    }

    @Test
    fun step2_11_pagesGoBackToTheirParent() {
        assertEquals(SettingsPage.HOME, SettingsPage.FAVORITES.parent)
        assertEquals(SettingsPage.APP_LIST, SettingsPage.HIDDEN_APPS.parent)
        assertEquals(SettingsPage.GESTURES, SettingsPage.PICK_SWIPE_RIGHT.parent)
        assertEquals(SettingsPage.ABOUT, SettingsPage.LICENSES.parent)
        assertEquals(SettingsPage.APPEARANCE, SettingsPage.PHONE_WALLPAPER.parent)
        assertEquals(SettingsPage.WELLBEING, SettingsPage.WELLBEING_APPS.parent)
        assertEquals(SettingsPage.MAIN, SettingsPage.BACKUP.parent)
    }

    @Test
    fun f1_3_f1_7_f1_8_f3_3_homeRows() {
        showHome(HomeSettings(maxFavorites = 6))
        compose.onNodeWithText("6").assertExists()
        compose.onNodeWithText("Left").assertExists()
        compose.onNodeWithText("Edit favorites").performClick()
        compose.onNodeWithText("Maximum favorites").performClick()
        compose.onNodeWithText("Alignment").performClick()
        compose.onNodeWithText("Show battery %").performClick()
        compose.onNodeWithText("Show next alarm").performClick()
        assertEquals(listOf("favorites", "max", "alignment", "battery:false", "alarm:true"), calls)
    }

    @Test
    fun f2_3_f2_5_f2_7_f5_2_f5_3_appListRows() {
        showAppList(AppListSettings(hiddenAppsConfirmation = HiddenAppsConfirmation.DELAY))
        compose.onNodeWithText("5 s delay").assertExists()
        compose.onNodeWithText("Open keyboard automatically").performClick()
        compose.onNodeWithText("Open single match automatically").performClick()
        compose.onNodeWithText("Alphabet scroller").performClick()
        compose.onNodeWithText("Hidden apps (2)").performClick()
        compose.onNodeWithText("Hidden apps confirmation").performClick()
        assertEquals(listOf("keyboard:false", "auto:true", "scroller:true", "hidden", "confirmation"), calls)
    }

    @Test
    fun f4_2_f4_3_f4_5_gestureRows() {
        showGestures(SettingsUiState(swipeLeftApp = AppEntry(AppKey("pkg.camera", "Main", 0), "Camera")))
        compose.onNodeWithText("Camera").assertExists()
        compose.onNodeWithText("Swipe left").performClick()
        compose.onNodeWithText("Swipe right").performClick()
        compose.onNodeWithText("Double-tap to lock").performClick()
        assertEquals(listOf("left", "right", "doubleTap:true"), calls)
    }

    @Test
    fun step4_2_warnsWhenAccessibilityIsOffOrStopped() {
        showGestures(SettingsUiState(settings = LauncherSettings(gestures = GestureSettings(doubleTapLock = true))))
        compose.onNodeWithText("Accessibility is off for ZeroOS. Tap to turn it on.").performClick()
        assertEquals(listOf("turnOn"), calls)
    }

    @Test
    fun step4_2_warnsWhenThePhoneStoppedTheService() {
        showGestures(SettingsUiState(accessibilityEnabled = true, accessibilityRunning = false))
        compose.onNodeWithText(
            "Accessibility is on but not running. Your phone may have stopped it: let ZeroOS run in the background (Settings → Battery)."
        ).assertExists()
    }

    @Test
    fun step4_2_noWarningWhileRunning() {
        showGestures(
            SettingsUiState(
                settings = LauncherSettings(gestures = GestureSettings(doubleTapLock = true)),
                accessibilityEnabled = true,
                accessibilityRunning = true
            )
        )
        compose.onNodeWithText("Accessibility is off for ZeroOS. Tap to turn it on.").assertDoesNotExist()
        compose.onNodeWithText("Accessibility is on but not running", substring = true).assertDoesNotExist()
    }

    @Test
    fun f4_4_swipeDownIsNotASetting() {
        showGestures()
        compose.onNodeWithText("Swipe down", substring = true).assertDoesNotExist()
    }

    @Test
    fun f4_6_pickerOffersOff() {
        compose.setContent {
            ZeroTheme {
                AppPickerScreen("Swipe left", "", emptyList(), onQueryChange = {}, onPick = {}, onBack = {}, onNone = { calls += "off" })
            }
        }
        compose.onNodeWithText("off").performClick()
        assertEquals(listOf("off"), calls)
    }

    @Test
    fun d037_aboutWordmarkScreenshot() {
        compose.setContent { ZeroTheme { AboutScreen(AboutState("0.0.12", 12), onLicenses = {}, onBack = {}) } }
        compose.onNodeWithContentDescription("ZeroOS").assertExists()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_aboutWordmarkScreenshot() {
        compose.setContent { ZeroTheme { AboutScreen(AboutState("0.0.12", 12), onLicenses = {}, onBack = {}) } }
        compose.onNodeWithContentDescription("ZeroOS").assertExists()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun d037_aboutWordmarkLightScreenshot() {
        compose.setContent { ZeroTheme(ThemeMode.LIGHT) { AboutScreen(AboutState("0.0.12", 12), onLicenses = {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_11_aboutShowsVersion() {
        compose.setContent { ZeroTheme { AboutScreen(AboutState("0.0.6", 6), onLicenses = { calls += "licenses" }, onBack = {}) } }
        compose.onNodeWithText("ZeroOS 0.0.6 (6)").assertExists()
        compose.onNodeWithText("Open-source licenses").performClick()
        assertEquals(listOf("licenses"), calls)
    }

    @Test
    fun step2_11_settingsMainScreenshot() {
        showMain()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_settingsMainScreenshot() {
        showMain()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_11_homeSettingsScreenshot() {
        showHome()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_11_appListSettingsScreenshot() {
        showAppList()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_11_gesturesScreenshot() {
        showGestures()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f6_1_settingsMainLightScreenshot() {
        showMain(ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f6_4_appListSettingsLargestTextScreenshot() {
        showAppList(AppListSettings(hiddenAppsConfirmation = HiddenAppsConfirmation.DELAY), TextSize.EXTRA_LARGE)
        compose.onRoot().captureRoboImage()
    }
}
