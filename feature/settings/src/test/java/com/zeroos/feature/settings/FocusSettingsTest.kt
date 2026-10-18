package com.zeroos.feature.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.model.settings.FocusSettings
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.preloadFocusMarks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
class FocusSettingsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val permissions = mockk<SpecialPermissions>(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f9_1_f9_7_f9_8_changesAreSaved() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        val vm = FocusSettingsViewModel(settings, permissions)
        assertEquals("Appendix A defaults", FocusSettings(), settings.settings.first().focus)
        vm.setShowOnHome(false)
        vm.setIcon(FocusIcon.FLOW)
        vm.onDndResult(granted = false)
        val saved = settings.settings.first { it.focus.icon == FocusIcon.FLOW && !it.focus.showOnHome }.focus
        assertFalse("declined: stays off", saved.doNotDisturb)
        vm.onDndResult(granted = true)
        settings.settings.first { it.focus.doNotDisturb }
        verify { permissions.record(SpecialPermission.DO_NOT_DISTURB, true) }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class FocusSettingsScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = FocusSettingsActions(
        onShowOnHome = { calls += "home $it" },
        onOpenIcons = { calls += "icons" },
        onStartFocus = { calls += "start" },
        onDoNotDisturb = { calls += "dnd $it" }
    )

    private fun preload() = preloadFocusMarks(ApplicationProvider.getApplicationContext<Context>().resources)

    @Test
    fun f9_10_focusPageRows() {
        compose.setContent { ZeroTheme { FocusSettingsScreen(FocusSettings(), actions, onBack = {}) } }
        listOf("Show \"focus\" on home", "Focus icon", "Start focus", "Turn on Do Not Disturb").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(listOf("home false", "icons", "start", "dnd true"), calls)
    }

    @Test
    fun f9_8_iconChooser() {
        var picked: FocusIcon? = null
        compose.setContent { ZeroTheme { FocusIconScreen(FocusIcon.MIX, { picked = it }, onBack = {}) } }
        compose.onNodeWithText("Rim").performClick()
        assertEquals(FocusIcon.RIM, picked)
    }

    @Test
    fun f9_10_focusRowShownEvenWhenWellbeingIsOff() {
        compose.setContent {
            ZeroTheme {
                WellbeingScreen(
                    WellbeingSettings(enabled = false),
                    WellbeingActions({ _, _ ->
                    }, {}, {}, { calls += "focus" }),
                    onBack = {}
                )
            }
        }
        compose.onNodeWithText("Focus").performClick()
        assertEquals(listOf("focus"), calls)
    }

    @Test
    fun step4_5_focusSettingsScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusSettingsScreen(FocusSettings(), actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_5_focusIconChooserScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusIconScreen(FocusIcon.SPHERE, {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_focusIconChooserScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusIconScreen(FocusIcon.MIX, {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_wellbeingScreenshot() {
        compose.setContent {
            ZeroTheme { WellbeingScreen(WellbeingSettings(enabled = true), WellbeingActions({ _, _ -> }, {}, {}), onBack = {}) }
        }
        compose.onRoot().captureRoboImage()
    }
}
