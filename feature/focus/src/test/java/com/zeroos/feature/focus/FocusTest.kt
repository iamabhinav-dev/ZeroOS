package com.zeroos.feature.focus

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.focus.FocusStateSerializer
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.model.settings.FocusSettings
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.preloadFocusMarks
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

private fun key(name: String) = AppKey("pkg.$name", "Main", 0)

@OptIn(ExperimentalCoroutinesApi::class)
class FocusPickerViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Phone", "Maps", "YouTube").map { LaunchableActivity(key(it), it) }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) = Unit
    }
    private lateinit var settings: SettingsRepository
    private lateinit var focus: FocusRepository

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.viewModel(initial: FocusSettings = FocusSettings()): FocusPickerViewModel {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(focus = initial) }
        settings = SettingsRepository(store, logger)
        focus = FocusRepository(
            DataStoreFactory.create(FocusStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            Clock.systemUTC(),
            logger
        )
        val vm =
            FocusPickerViewModel(
                AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
                settings,
                focus,
                SavedStateHandle()
            )
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.apps.isNotEmpty() && it.minutes != null }
        return vm
    }

    @Test
    fun f9_2_lastChoiceIsTheDefault() = runTest {
        val vm = viewModel(FocusSettings(lastMinutes = 50, lastAllowed = listOf(key("Maps").asString(), "gone/X#0")))
        val state = vm.state.value
        assertEquals(50, state.minutes)
        assertEquals("uninstalled apps are dropped", listOf(key("Maps").asString()), state.allowed)
    }

    @Test
    fun f9_2_presetsCustomStepsAndPickingApps() = runTest {
        val vm = viewModel()
        vm.selectMinutes(90)
        assertEquals(90, vm.state.first { it.minutes == 90 }.minutes)
        // Tapping the Custom row picks the custom length it shows (45 by default).
        vm.selectCustom()
        assertEquals(45, vm.state.first { it.minutes == 45 }.minutes)
        vm.selectMinutes(25)
        vm.stepCustom(longer = true)
        assertEquals("‹ › step from the custom length and pick it", 50, vm.state.first { it.minutes == 50 && it.custom == 50 }.minutes)
        vm.stepCustom(longer = false)
        assertEquals("one step back", 45, vm.state.first { it.minutes == 45 }.minutes)
        repeat(30) { vm.stepCustom(longer = true) }
        assertEquals("stops at 240", 240, vm.state.first { it.custom == 240 }.minutes)
        repeat(30) { vm.stepCustom(longer = false) }
        assertEquals("stops at 5", 5, vm.state.first { it.custom == 5 }.minutes)
        vm.onQueryChange("ma")
        assertEquals(listOf("Maps"), vm.state.first { it.query == "ma" }.apps.map { it.label })
        vm.onQueryChange("")
        val phone = vm.state.value.apps.first { it.label == "Phone" }
        vm.toggle(phone)
        assertEquals(listOf(phone.key.asString()), vm.state.first { it.allowed.isNotEmpty() }.allowed)
        vm.toggle(phone)
        assertEquals(emptyList<String>(), vm.state.first { it.allowed.isEmpty() }.allowed)
    }

    @Test
    fun f9_1_startRemembersTheChoiceAndStartsFocus() = runTest {
        val vm = viewModel()
        vm.selectMinutes(50)
        vm.toggle(vm.state.first { it.minutes == 50 }.apps.first { it.label == "Maps" })
        vm.state.first { it.allowed.isNotEmpty() }
        var started: Int? = null
        vm.start { started = it }
        val session = focus.active.first { it != null }!!
        assertEquals(setOf(key("Maps").asString()), session.allowed)
        assertEquals(50 * 60_000L, session.endsAtMillis - session.startedAtMillis)
        assertEquals(50, started)
        val saved = settings.settings.first().focus
        assertEquals(50, saved.lastMinutes)
        assertEquals(listOf(key("Maps").asString()), saved.lastAllowed)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class FocusScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val apps = listOf("Phone", "Messages", "Maps", "YouTube").map { AppEntry(key(it), it) }
    private val state = FocusPickerState(FocusIcon.MIX, 50, apps, listOf(key("Phone").asString(), key("Messages").asString()))
    private val actions = FocusPickerActions(
        onPreset = { calls += "preset $it" },
        onCustom = { calls += if (it) "longer" else "shorter" },
        onSelectCustom = { calls += "custom" },
        onQueryChange = { calls += "query $it" },
        onToggle = { calls += "toggle ${it.label}" },
        onStart = { calls += "start" }
    )

    private fun preload() = preloadFocusMarks(ApplicationProvider.getApplicationContext<Context>().resources)

    @Test
    fun f9_2_pickerActions() {
        compose.setContent { ZeroTheme { FocusPickerScreen(state, actions, onBack = {}) } }
        compose.onNodeWithText("25 min").performClick()
        compose.onNodeWithContentDescription("Longer").performClick()
        compose.onNodeWithContentDescription("Shorter").performClick()
        compose.onNodeWithText("Custom").performClick()
        compose.onNodeWithText("Maps").performClick()
        compose.onNodeWithTag(FocusTestTags.START).performClick()
        assertEquals(listOf("preset 25", "longer", "shorter", "custom", "toggle Maps", "start"), calls)
        // Picked apps show their place.
        compose.onNodeWithText("2").assertExists()
    }

    @Test
    fun f9_9b_startedScreenGoesAfterTwoSecondsOrOnTap() {
        compose.mainClock.autoAdvance = false
        var done = 0
        compose.setContent { ZeroTheme { FocusStartedScreen(FocusIcon.MIX, 50, onDone = { done++ }) } }
        compose.onNodeWithText("Focus · 50 min").assertExists()
        compose.mainClock.advanceTimeBy(1_900)
        assertEquals(0, done)
        compose.mainClock.advanceTimeBy(200)
        assertEquals(1, done)
        compose.onNodeWithTag(FocusTestTags.STARTED).performClick()
        assertEquals(2, done)
    }

    @Test
    fun step4_5_pickerScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusPickerScreen(state, actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_5_startedScreenshotLight() {
        preload()
        compose.mainClock.autoAdvance = false
        compose.setContent { ZeroTheme(ThemeMode.LIGHT) { FocusStartedScreen(FocusIcon.SPHERE, 50, onDone = {}) } }
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_pickerScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusPickerScreen(state, actions, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_startedScreenshot() {
        preload()
        compose.mainClock.autoAdvance = false
        compose.setContent { ZeroTheme { FocusStartedScreen(FocusIcon.MIX, 50, onDone = {}) } }
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f9_5_endingEarlyNeedsAReason() {
        var ended = 0
        var kept = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { ZeroTheme { FocusEndScreen(FocusIcon.MIX, onEnd = { ended++ }, onKeep = { kept++ }) } }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("End focus early?").assertExists()
        compose.onNodeWithText("End focus").assertDoesNotExist()
        compose.onNodeWithTag(FocusTestTags.REASON).performTextInput("   ")
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("End focus").assertDoesNotExist()
        compose.onNodeWithTag(FocusTestTags.REASON).performTextInput("call from work")
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("End focus").performClick()
        compose.onNodeWithText("Keep focusing").performClick()
        assertEquals(1, ended)
        assertEquals(1, kept)
    }

    @Test
    fun step4_5_endEarlyScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusEndScreen(FocusIcon.MIX, onEnd = {}, onKeep = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_endEarlyScreenshot() {
        preload()
        compose.setContent { ZeroTheme { FocusEndScreen(FocusIcon.MIX, onEnd = {}, onKeep = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun d052_pickerBackArrow() {
        var backs = 0
        compose.setContent { ZeroTheme { FocusPickerScreen(state, actions, onBack = { backs++ }) } }
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }
}
