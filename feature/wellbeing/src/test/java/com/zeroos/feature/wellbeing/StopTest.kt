package com.zeroos.feature.wellbeing

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.data.wellbeing.StopScreen
import com.zeroos.core.data.wellbeing.TodayUsage
import com.zeroos.core.data.wellbeing.UsageEvent
import com.zeroos.core.data.wellbeing.UsageEventSource
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.data.wellbeing.WellbeingStatus
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.testing.MutableClock
import com.zeroos.core.ui.ZeroTheme
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val instagram = AppEntry(AppKey("com.instagram", "Main", 0), "Instagram")

@OptIn(ExperimentalCoroutinesApi::class)
class StopViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val launched = mutableListOf<String>()
    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = emptyList<LaunchableActivity>()

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) {
            launched += key.packageName
        }
    }
    private val clock = MutableClock(Instant.parse("2026-10-17T10:00:00Z"))
    private lateinit var state: WellbeingStateRepository

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Instagram used for [usedMinutes] today, then opened from ZeroOS. */
    private suspend fun TestScope.opened(wellbeing: WellbeingSettings, usedMinutes: Long = 0): Pair<StopViewModel, LaunchGate> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(wellbeing = wellbeing) }
        val settings = SettingsRepository(store, logger)
        settings.settings.first()
        val io = UnconfinedTestDispatcher(testScheduler)
        state = WellbeingStateRepository(
            DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            clock,
            logger,
            zone = clock::getZone
        )
        val events = UsageEventSource { _, _ ->
            listOf(UsageEvent("com.instagram", UsageEvent.Type.PAUSED, clock.millis()))
                .takeIf { usedMinutes > 0 }
                ?.let { listOf(UsageEvent("com.instagram", UsageEvent.Type.RESUMED, clock.millis() - usedMinutes * 60_000)) + it }
                .orEmpty()
        }
        val gate = LaunchGate(AppsRepository(source, logger, io), settings, backgroundScope, logger)
        val vm = StopViewModel(gate, settings, WellbeingStatus(TodayUsage(events, clock, io, logger, clock::getZone), state), state, logger)
        gate.launch(instagram)
        assertEquals(instagram, vm.pending.value)
        vm.check(instagram, PunCounts(20, 20))
        return vm to gate
    }

    private val rules = mapOf("com.instagram" to AppRule(pause = true, dailyLimitMinutes = 30))

    @Test
    fun f8_1_pauseThenOpenAnyway() = runTest(UnconfinedTestDispatcher()) {
        val (vm, gate) = opened(WellbeingSettings(enabled = true, apps = rules))
        val shown = vm.state.first { it.screen != null }
        assertEquals(StopScreen.Pause(5, false), shown.screen)
        assertTrue(shown.pun in 0 until 20)
        assertTrue(launched.isEmpty())
        vm.openAnyway()
        assertEquals(listOf("com.instagram"), launched)
        assertNull(gate.stopFor.value)
        assertTrue(logged.containsAll(listOf("pause shown for com.instagram (5 s)", "pause: opened com.instagram")))
    }

    @Test
    fun f8_4_limitReachedThenFiveMoreMinutes() = runTest(UnconfinedTestDispatcher()) {
        val (vm, _) = opened(WellbeingSettings(enabled = true, apps = rules), usedMinutes = 30)
        assertEquals(StopScreen.Limit(30, 2), vm.state.first { it.screen != null }.screen)
        vm.moreMinutes()
        assertEquals(mapOf("com.instagram" to 1), state.extrasUsedToday())
        assertEquals(listOf("com.instagram"), launched)
    }

    @Test
    fun f8_4_goBackKeepsTheAppClosed() = runTest(UnconfinedTestDispatcher()) {
        val (vm, gate) = opened(WellbeingSettings(enabled = true, apps = rules), usedMinutes = 45)
        vm.state.first { it.screen != null }
        vm.goBack()
        assertNull(gate.stopFor.value)
        assertTrue(launched.isEmpty())
        assertTrue(logged.contains("stop: went back from com.instagram"))
    }

    @Test
    fun f8_3_underTheLimitWithoutPauseOpensStraightAway() = runTest(UnconfinedTestDispatcher()) {
        val limitOnly = mapOf("com.instagram" to AppRule(dailyLimitMinutes = 30))
        opened(WellbeingSettings(enabled = true, apps = limitOnly), usedMinutes = 10)
        assertEquals(listOf("com.instagram"), launched)
    }

    @Test
    fun f8_14_punsOffGivesNoLine() = runTest(UnconfinedTestDispatcher()) {
        val (vm, _) = opened(WellbeingSettings(enabled = true, puns = false, apps = rules))
        assertNull(vm.state.first { it.screen != null }.pun)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class StopScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = StopActions(onOpen = { calls += "open" }, onMoreMinutes = { calls += "more" }, onBack = { calls += "back" })

    private fun show(screen: StopScreen, pun: Int? = 2, theme: ThemeMode = ThemeMode.DARK) =
        compose.setContent { ZeroTheme(theme) { StopScreen(StopUiState(instagram, screen, pun), actions) } }

    @Test
    fun f8_1_countdownThenOpen() {
        compose.mainClock.autoAdvance = false
        show(StopScreen.Pause(5, intentionPrompt = false))
        compose.onNodeWithText("Is this a want, or a habit?").assertExists()
        compose.onNodeWithText("5").assertExists()
        compose.onNodeWithText("Open Instagram").assertDoesNotExist()
        compose.onNodeWithTag(StopTestTags.INTENTION).assertDoesNotExist()
        compose.mainClock.advanceTimeBy(5_100)
        compose.onNodeWithText("Open Instagram").performClick()
        compose.onNodeWithText("Go back").performClick()
        assertEquals(listOf("open", "back"), calls)
    }

    @Test
    fun f8_2_intentionFieldWhenPromptIsOn() {
        show(StopScreen.Pause(5, intentionPrompt = true))
        compose.onNodeWithText("Why are you opening it?").assertExists()
    }

    @Test
    fun f8_4_limitScreenWithUsesLeftAndHiddenAtZero() {
        var screen by mutableStateOf<StopScreen>(StopScreen.Limit(30, 2))
        compose.setContent { ZeroTheme { StopScreen(StopUiState(instagram, screen, 0), actions) } }
        compose.onNodeWithText("That's a wrap for today.").assertExists()
        compose.onNodeWithText("Instagram · 30 min used today").assertExists()
        compose.onNodeWithText("5 more minutes (2)").performClick()
        compose.onNodeWithText("Go home").performClick()
        assertEquals(listOf("more", "back"), calls)
        screen = StopScreen.Limit(40, 0)
        compose.onNodeWithText("5 more minutes", substring = true).assertDoesNotExist()
    }

    @Test
    fun f8_14_plainMessagesWithoutPuns() {
        var screen by mutableStateOf<StopScreen>(StopScreen.Pause(3, false))
        compose.setContent { ZeroTheme { StopScreen(StopUiState(instagram, screen, null), actions) } }
        compose.onNodeWithText("Take a breath first.").assertExists()
        screen = StopScreen.Limit(30, 1)
        compose.onNodeWithText("Daily limit reached").assertExists()
    }

    @Test
    fun step4_3_pauseScreenshot() {
        compose.mainClock.autoAdvance = false
        show(StopScreen.Pause(5, intentionPrompt = true))
        compose.mainClock.advanceTimeBy(2_500)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_3_limitScreenshotLight() {
        show(StopScreen.Limit(30, 1), pun = 0, theme = ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }
}
