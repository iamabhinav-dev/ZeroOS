package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.model.settings.FocusSettings
import com.zeroos.core.model.settings.ScreenTimeSettings
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.ui.DocumentReader
import com.zeroos.core.ui.ZeroTheme
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** D-052: every page's back arrow does what Back does, i.e. goes to the page it was opened from. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class BackArrowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = AppEntry(AppKey("pkg.maps", "Main", 0), "Maps")

    private fun tapsBack(page: @Composable (onBack: () -> Unit) -> Unit) {
        var backs = 0
        compose.setContent { ZeroTheme { page { backs++ } } }
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun d052_settingsMain() = tapsBack { SettingsMainScreen(onOpen = {}, onOpenDiagnostics = {}, onBack = it) }

    @Test
    fun d052_wellbeing() = tapsBack {
        WellbeingScreen(WellbeingSettings(enabled = true), WellbeingActions({ _, _ -> }, {}, {}), onBack = it)
    }

    @Test
    fun d052_wellbeingApps() = tapsBack { WellbeingAppsScreen(WellbeingUiState(), {}, { _, _, _ -> }, {}, onBack = it) }

    @Test
    fun d052_focusSettings() = tapsBack { FocusSettingsScreen(FocusSettings(), FocusSettingsActions({}, {}, {}, {}), onBack = it) }

    @Test
    fun d052_focusIcon() = tapsBack { FocusIconScreen(FocusIcon.MIX, {}, onBack = it) }

    @Test
    fun d052_hiddenApps() = tapsBack { HiddenAppsScreen(listOf(app), onUnhide = {}, onBack = it) }

    @Test
    fun d052_appearance() = tapsBack { AppearanceScreen(AppearanceSettings(), mockk(relaxed = true), onBack = it) }

    @Test
    fun d052_backup() = tapsBack { BackupScreen(BackupState(), {}, {}, {}, {}, onBack = it) }

    @Test
    fun d052_favorites() = tapsBack { FavoritesEditorScreen(FavoritesEditorState(listOf(app), 6), {}, { _, _ -> }, {}, onBack = it) }

    @Test
    fun d052_appPicker() = tapsBack { AppPickerScreen("Swipe left", "", listOf(app), {}, {}, onBack = it) }

    @Test
    fun d052_phoneWallpaper() = tapsBack {
        PhoneWallpaperScreen(PhoneWallpaperState(), mockk(relaxed = true), { _, _, _ -> null }, onBack = it)
    }

    @Test
    fun d052_screenTime() = tapsBack { ScreenTimeSettingsScreen(ScreenTimeSettings(), ScreenTimeActions({}, {}, {}, {}, {}), onBack = it) }

    @Test
    fun d052_shortVideos() = tapsBack {
        ShortFormSettingsScreen(ShortFormUiState(), ShortFormActions({}, { _, _ -> }, {}, {}, {}), onBack = it)
    }

    @Test
    fun d052_about() = tapsBack { AboutScreen(AboutState(), onLicenses = {}, onBack = it) }

    @Test
    fun d052_document() = tapsBack { DocumentReader("License", "Text", onBack = it) }

    @Test
    fun d052_licenses() = tapsBack { LicensesScreen("", onBack = it) }

    @Test
    fun d052_everyPageGoesBackToItsParentNotHome() {
        val expected = mapOf(
            SettingsPage.HOME to SettingsPage.MAIN,
            SettingsPage.FAVORITES to SettingsPage.HOME,
            SettingsPage.APP_LIST to SettingsPage.MAIN,
            SettingsPage.HIDDEN_APPS to SettingsPage.APP_LIST,
            SettingsPage.GESTURES to SettingsPage.MAIN,
            SettingsPage.PICK_SWIPE_LEFT to SettingsPage.GESTURES,
            SettingsPage.PICK_SWIPE_RIGHT to SettingsPage.GESTURES,
            SettingsPage.APPEARANCE to SettingsPage.MAIN,
            SettingsPage.PHONE_WALLPAPER to SettingsPage.APPEARANCE,
            SettingsPage.WELLBEING to SettingsPage.MAIN,
            SettingsPage.WELLBEING_APPS to SettingsPage.WELLBEING,
            SettingsPage.FOCUS to SettingsPage.WELLBEING,
            SettingsPage.FOCUS_ICON to SettingsPage.FOCUS,
            SettingsPage.SCREEN_TIME to SettingsPage.MAIN,
            SettingsPage.SHORT_VIDEOS to SettingsPage.WELLBEING,
            SettingsPage.BACKUP to SettingsPage.MAIN,
            SettingsPage.ABOUT to SettingsPage.MAIN,
            SettingsPage.LICENSES to SettingsPage.ABOUT,
            SettingsPage.DOC_README to SettingsPage.ABOUT,
            SettingsPage.DOC_LICENSE to SettingsPage.ABOUT,
            SettingsPage.DOC_NOTICES to SettingsPage.ABOUT
        )
        assertEquals(SettingsPage.entries.toSet() - SettingsPage.MAIN, expected.keys)
        expected.forEach { (page, parent) -> assertEquals(page.name, parent, page.parent) }
    }
}
