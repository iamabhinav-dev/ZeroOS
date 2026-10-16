package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.Wallpaper
import com.zeroos.core.ui.ZeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class AppearanceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val temp = TemporaryFolder()

    private companion object {
        /** 15 Oct 2026, 05:12 UTC: a fixed preview time keeps screenshots stable. */
        const val FIXED_TIME = 1_792_041_120_000L
    }

    private val calls = mutableListOf<String>()
    private val actions = AppearanceActions(
        onTheme = { calls += "theme" },
        onWallpaper = { calls += "wallpaper" },
        onTextSize = { calls += "size" },
        onShowClock = { calls += "clock:$it" },
        onShowDate = { calls += "date:$it" },
        onShowStatusBar = { calls += "status:$it" }
    )

    private fun show(state: AppearanceSettings) = compose.setContent {
        ZeroTheme { AppearanceScreen(state, actions, onBack = {}, previewTime = java.util.Date(FIXED_TIME)) }
    }

    @Test
    fun f6_showsCurrentValuesAndPreview() {
        show(AppearanceSettings(theme = ThemeMode.LIGHT, wallpaper = Wallpaper.SYSTEM, textSize = TextSize.LARGE))
        listOf("Light", "Phone wallpaper", "Large").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithTag(AppearanceTestTags.PREVIEW).assertExists()
    }

    @Test
    fun f6_rowsCallActions() {
        show(AppearanceSettings())
        listOf("Theme", "Wallpaper", "Text size", "Show clock", "Show date", "Show status bar").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(listOf("theme", "wallpaper", "size", "clock:false", "date:false", "status:false"), calls)
    }

    @Test
    fun step2_8_appearanceDarkScreenshot() {
        show(AppearanceSettings())
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_8_appearanceLightPreviewScreenshot() {
        show(AppearanceSettings(theme = ThemeMode.LIGHT, textSize = TextSize.EXTRA_LARGE))
        compose.onRoot().captureRoboImage()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f6_1_f6_2_f6_4_f6_5_changesAreSaved() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val vm = AppearanceViewModel(SettingsRepository(store, logger))
        backgroundScope.launch { vm.state.collect {} }
        vm.nextTheme()
        vm.state.first { it.theme == ThemeMode.LIGHT }
        vm.nextWallpaper()
        vm.state.first { it.wallpaper == Wallpaper.SYSTEM }
        vm.nextTextSize()
        vm.state.first { it.textSize == TextSize.LARGE }
        vm.setShowClock(false)
        vm.setShowDate(false)
        vm.setShowStatusBar(false)
        val saved = store.data.first { !it.appearance.showStatusBar }.appearance
        assertEquals(
            AppearanceSettings(
                ThemeMode.LIGHT,
                Wallpaper.SYSTEM,
                TextSize.LARGE,
                showClock = false,
                showDate = false,
                showStatusBar = false
            ),
            saved
        )
    }
}
