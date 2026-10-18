package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ScreenTimeSettings
import com.zeroos.core.model.settings.StatsStyle
import com.zeroos.core.ui.ZeroTheme
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenTimeSettingsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val permissions = mockk<SpecialPermissions>(relaxed = true)

    private fun newFile() = temp.newFile().apply { delete() }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f10_3_f10_5_f10_7_f10_8_defaultsAndChanges() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = { newFile() })
        val settings = SettingsRepository(store, logger)
        val vm = ScreenTimeSettingsViewModel(settings, permissions)
        val defaults = settings.settings.first().screenTime
        assertTrue("on by default (Appendix A)", defaults.showOnHome)
        assertEquals(StatsStyle.TEXT, defaults.style)
        assertEquals(null, defaults.goalMinutes)
        assertFalse(defaults.compareYesterday)

        vm.cycleStyle()
        vm.cycleGoal()
        vm.setCompare(true)
        val changed = settings.settings.first { it.screenTime.compareYesterday && it.screenTime.goalMinutes != null }.screenTime
        assertEquals(StatsStyle.PROGRESS, changed.style)
        assertEquals("2 h when turned on", 120, changed.goalMinutes)
        repeat(GOAL_STEPS.size - 1) { vm.cycleGoal() }
        assertEquals("back to off", null, settings.settings.first { it.screenTime.goalMinutes == null }.screenTime.goalMinutes)
    }

    @Test
    fun f10_11_dismissedHintComesBackWhenTurnedOnAgain() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = { newFile() })
        store.updateData { it.copy(screenTime = ScreenTimeSettings(hintDismissed = true)) }
        val settings = SettingsRepository(store, logger)
        val vm = ScreenTimeSettingsViewModel(settings, permissions)
        vm.onUsageAccessResult(granted = true)
        assertFalse(settings.settings.first { !it.screenTime.hintDismissed }.screenTime.hintDismissed)
        verify { permissions.record(SpecialPermission.USAGE_ACCESS, true) }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class ScreenTimeSettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = ScreenTimeActions(
        onShowOnHome = { calls += "show $it" },
        onStyle = { calls += "style" },
        onGoal = { calls += "goal" },
        onCompare = { calls += "compare $it" },
        onInsights = { calls += "insights" }
    )

    @Test
    fun f10_screenTimePageRows() {
        compose.setContent { ZeroTheme { ScreenTimeSettingsScreen(ScreenTimeSettings(goalMinutes = 150), actions, onBack = {}) } }
        listOf("Show on home", "Style", "Daily goal", "Compare with yesterday", "Insights").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(listOf("show false", "style", "goal", "compare true", "insights"), calls)
        compose.onNodeWithText("2h 30m").assertExists()
        compose.onNodeWithText("Text").assertExists()
    }

    @Test
    fun step4_6_screenTimeSettingsScreenshot() {
        compose.setContent { ZeroTheme { ScreenTimeSettingsScreen(ScreenTimeSettings(goalMinutes = 120), actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_screenTimeSettingsScreenshot() {
        compose.setContent { ZeroTheme { ScreenTimeSettingsScreen(ScreenTimeSettings(), actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }
}
