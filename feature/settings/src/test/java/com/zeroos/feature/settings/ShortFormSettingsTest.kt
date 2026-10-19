package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ShortFormMode
import com.zeroos.core.model.settings.ShortFormSettings
import com.zeroos.core.model.settings.YOUTUBE_PACKAGE
import com.zeroos.core.ui.ZeroTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
class ShortFormSettingsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val permissions = mockk<SpecialPermissions>(relaxed = true)
    private val actions = mockk<SystemActions> { every { running } returns MutableStateFlow(true) }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newFile() = temp.newFile().apply { delete() }

    @Test
    fun f16_1_f16_4_f16_8_f16_10_f16_11_defaultsAndChanges() = runTest {
        val settings =
            SettingsRepository(DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = { newFile() }), logger)
        val vm = ShortFormSettingsViewModel(settings, permissions, actions)
        val d = settings.settings.first().shortForm
        assertFalse("off by default", d.enabled)
        assertEquals(ShortFormMode.BLOCK, d.mode)
        assertEquals(15, d.allowanceMinutes)
        assertEquals(1, d.allowUsesPerDay)

        vm.onAccessibilityResult(granted = true)
        vm.setApp(YOUTUBE_PACKAGE, false)
        vm.cycle(ShortFormCycle.MODE)
        vm.cycle(ShortFormCycle.ALLOWANCE)
        vm.cycle(ShortFormCycle.AUTO_HOME)
        vm.cycle(ShortFormCycle.ALLOW_USES)
        val s = settings.settings.first { it.shortForm.allowUsesPerDay == 2 }.shortForm
        assertTrue(s.enabled)
        assertFalse(YOUTUBE_PACKAGE in s.blockedApps)
        assertEquals(ShortFormMode.LIMIT, s.mode)
        assertEquals(20, s.allowanceMinutes)
        assertEquals(3, s.autoHomeSeconds)
        verify { permissions.record(SpecialPermission.ACCESSIBILITY, true) }
    }

    @Test
    fun f16_9_f17_8_customMessageCappedAndNeverLogged() = runTest {
        val settings =
            SettingsRepository(DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = { newFile() }), logger)
        val vm = ShortFormSettingsViewModel(settings, permissions, actions)
        vm.setCustomMessage("Secret plans " + "x".repeat(200))
        val s = settings.settings.first { it.shortForm.useCustomMessage }.shortForm
        assertEquals(120, s.customMessage.length)
        assertTrue(logged.none { "Secret" in it })
        assertTrue(logged.any { "<redacted, 120 chars>" in it })
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class ShortFormSettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = ShortFormActions(
        onEnabled = { calls += "enabled $it" },
        onApp = { p, on -> calls += "app $p $on" },
        onCycle = { calls += it.name },
        onEditMessage = { calls += "edit" },
        onTurnOnAccessibility = { calls += "accessibility" }
    )

    private val on = ShortFormUiState(ShortFormSettings(enabled = true, mode = ShortFormMode.LIMIT, useCustomMessage = true), true, true)

    @Test
    fun f16_rowsShowOnlyWhileOn() {
        compose.setContent { ZeroTheme { ShortFormSettingsScreen(ShortFormUiState(), actions, onBack = {}) } }
        compose.onNodeWithText("YouTube Shorts").assertDoesNotExist()
        compose.onNodeWithText("Block short videos").performClick()
        assertEquals(listOf("enabled true"), calls)
    }

    @Test
    fun f16_4_f16_8_f16_9_rowsWhenOn() {
        compose.setContent { ZeroTheme { ShortFormSettingsScreen(on, actions, onBack = {}) } }
        listOf("YouTube Shorts", "Mode", "Daily allowance", "Message", "Custom message", "Go home automatically").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(listOf("app $YOUTUBE_PACKAGE false", "MODE", "ALLOWANCE", "MESSAGE", "edit", "AUTO_HOME"), calls)
    }

    @Test
    fun f16_15_warnsWhenAccessibilityIsOffOrStopped() {
        var state by mutableStateOf(on.copy(accessibilityOn = false))
        compose.setContent { ZeroTheme { ShortFormSettingsScreen(state, actions, onBack = {}) } }
        compose.onNodeWithText("Accessibility is off for ZeroOS. Tap to turn it on.").performClick()
        assertEquals(listOf("accessibility"), calls)
        state = on.copy(running = false)
        compose.onNodeWithText("Accessibility is on but not running", substring = true).assertExists()
    }

    @Test
    fun f16_9_customMessageDialogCounts() {
        var saved: String? = null
        compose.setContent { ZeroTheme { CustomMessageDialog("", onSave = { saved = it }, onCancel = {}) } }
        compose.onNodeWithText("120 left").assertExists()
        compose.onNodeWithTag(SettingsTestTags.SHORT_MESSAGE).performTextInput("Go outside")
        compose.onNodeWithText("110 left").assertExists()
        compose.onNodeWithText("Save").performClick()
        assertEquals("Go outside", saved)
    }

    @Test
    fun step6_3_shortVideosSettingsScreenshot() {
        compose.setContent { ZeroTheme { ShortFormSettingsScreen(on, actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_shortVideosSettingsScreenshot() {
        compose.setContent { ZeroTheme { ShortFormSettingsScreen(on, actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }
}
