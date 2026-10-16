package com.zeroos.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.Wallpaper
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStorageTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<Pair<LogLevel, String>>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += level to message
        }
    }

    private fun kotlinx.coroutines.test.TestScope.storeAt(file: File) =
        DataStoreFactory.create(serializer = SettingsSerializer, scope = backgroundScope, produceFile = { file })

    @Test
    fun step2_1_defaultsMatchAppendixA() {
        val s = LauncherSettings()
        assertEquals(LauncherSettings.CURRENT_VERSION, s.version)
        assertFalse(s.onboardingCompleted)
        assertTrue(s.favorites.isEmpty() && s.hiddenApps.isEmpty() && s.renamedApps.isEmpty())
        assertEquals(6, s.home.maxFavorites)
        assertEquals(Alignment.LEFT, s.home.alignment)
        assertTrue(s.home.showBattery)
        assertFalse(s.home.showNextAlarm)
        assertTrue(s.appList.keyboardAutoOpen)
        assertFalse(s.appList.autoLaunchSingleMatch)
        assertFalse(s.appList.fastScroller)
        assertEquals(HiddenAppsConfirmation.OFF, s.appList.hiddenAppsConfirmation)
        assertNull(s.gestures.swipeLeftApp)
        assertNull(s.gestures.swipeRightApp)
        assertEquals(ThemeMode.DARK, s.appearance.theme)
        assertEquals(Wallpaper.BLACK, s.appearance.wallpaper)
        assertEquals(TextSize.MEDIUM, s.appearance.textSize)
        assertTrue(s.appearance.showClock && s.appearance.showDate && s.appearance.showStatusBar)
    }

    @Test
    fun step2_1_changesAreSavedAndLogged() = runTest {
        val file = File(temp.root, "settings.json")
        val repository = SettingsRepository(storeAt(file), logger)
        repository.update("home.alignment=CENTER") { it.copy(home = it.home.copy(alignment = Alignment.CENTER)) }

        assertEquals(Alignment.CENTER, repository.settings.first().home.alignment)
        assertTrue(file.readText().contains("\"alignment\": \"CENTER\""))
        assertEquals(LogLevel.INFO to "setting changed: home.alignment=CENTER", logged.single())
    }

    @Test
    fun step2_1_migrationOlderFileGetsDefaultsForMissingFields() = runTest {
        val file = File(temp.root, "settings.json").apply { writeText("""{ "version": 1, "favorites": ["a/a.Main#0"] }""") }
        val settings = storeAt(file).data.first()
        assertEquals(listOf("a/a.Main#0"), settings.favorites)
        assertEquals(LauncherSettings().home, settings.home)
    }

    @Test
    fun step2_1_newerFileWithUnknownFieldsStillLoads() = runTest {
        val file = File(temp.root, "settings.json").apply {
            writeText("""{ "version": 2, "futureFeature": { "x": 1 }, "home": { "alignment": "RIGHT", "newOption": true } }""")
        }
        assertEquals(Alignment.RIGHT, storeAt(file).data.first().home.alignment)
    }

    @Test
    fun f4_4_removedSwipeDownSwitchInOldFilesIsIgnored() = runTest {
        val file = File(temp.root, "old-gestures.json").apply {
            writeText("""{ "gestures": { "swipeLeftApp": "a/a.Main#0", "swipeDownEnabled": false, "swipeDownAction": "SEARCH" } }""")
        }
        assertEquals("a/a.Main#0", storeAt(file).data.first().gestures.swipeLeftApp)
    }

    @Test
    fun step2_1_unknownEnumValueFallsBackToDefault() = runTest {
        val file = File(temp.root, "settings.json").apply { writeText("""{ "appearance": { "theme": "NEON" } }""") }
        assertEquals(ThemeMode.DARK, storeAt(file).data.first().appearance.theme)
    }

    @Test
    fun step2_1_corruptedFileResetsToDefaultsAndIsLogged() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.dataStoreFile(SETTINGS_FILE_NAME).apply { parentFile?.mkdirs() }.writeText("{ not json")
        val settings = createSettingsDataStore(context, logger, backgroundScope).data.first()
        assertEquals(LauncherSettings(), settings)
        assertEquals(LogLevel.ERROR to "settings file corrupted; reset to defaults", logged.single())
    }
}
