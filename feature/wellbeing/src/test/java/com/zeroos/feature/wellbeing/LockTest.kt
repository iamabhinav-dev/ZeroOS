package com.zeroos.feature.wellbeing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.focus.FocusStateSerializer
import com.zeroos.core.data.lock.LockUnlockStore
import com.zeroos.core.data.lock.ScreenTimeLock
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ScreenTimeLockSettings
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.testing.MutableClock
import com.zeroos.core.ui.ZeroTheme
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

@OptIn(ExperimentalCoroutinesApi::class)
class LockViewModelTest {
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
    private val clock = MutableClock(Instant.parse("2026-11-01T14:00:00Z"))
    private val video = AppEntry(AppKey("com.example.video", "Main", 0), "Video")
    private val puns = LockPunCounts(12, 12)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Past today's 4 h cap, Video tapped on home. */
    private suspend fun TestScope.locked(): Pair<LockViewModel, LaunchGate> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(screenTimeLock = ScreenTimeLockSettings(enabled = true)) }
        val settings = SettingsRepository(store, logger)
        settings.settings.first()
        val stateStore = DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val lock = ScreenTimeLock(settings, { 250 * 60_000L }, LockUnlockStore(stateStore), clock, backgroundScope, logger) { emptySet() }
        lock.refresh()
        val focus = FocusRepository(
            DataStoreFactory.create(FocusStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            clock,
            logger
        )
        val gate =
            LaunchGate(
                AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
                settings,
                focus,
                backgroundScope,
                logger,
                lock
            )
        val vm =
            LockViewModel(
                gate,
                lock,
                settings,
                WellbeingStateRepository(stateStore, clock, logger, zone = clock::getZone),
                SavedStateHandle()
            )
        gate.launch(video)
        assertEquals(video, vm.pending.value)
        vm.show(puns)
        runCurrent()
        return vm to gate
    }

    @Test
    fun f19_3_lockScreenShowsTodaysTimeAndThePhrase() = runTest {
        val (vm, _) = locked()
        assertEquals(250 * 60_000L, vm.state.value.usedMillis)
        assertEquals("IamWeakIneedThis", vm.state.value.phrase)
        assertEquals(LockPage.LOCKED, vm.state.value.page)
    }

    @Test
    fun f19_4_rightPhraseOpensTheApp() = runTest {
        val (vm, gate) = locked()
        vm.openUnlock()
        assertEquals(LockPage.UNLOCK, vm.state.value.page)
        vm.submit("IamWeakIneedThis", puns)
        runCurrent()
        assertEquals(listOf("com.example.video"), launched)
        assertNull(gate.lockFor.value)
        assertEquals(LockPage.LOCKED, vm.state.value.page)
    }

    @Test
    fun f19_5_wrongPhraseShowsThePunAndTryAgainReturnsToTheField() = runTest {
        val (vm, gate) = locked()
        vm.openUnlock()
        vm.submit("IamStrong", puns)
        runCurrent()
        assertEquals(LockPage.WRONG, vm.state.value.page)
        assertTrue(launched.isEmpty())
        assertEquals(video, gate.lockFor.value)
        vm.tryAgain()
        assertEquals(LockPage.UNLOCK, vm.state.value.page)
        assertTrue("typed text never logged (F17.8)", logged.none { it.contains("IamStrong") })
    }

    @Test
    fun f19_4_backFromUnlockReturnsToTheLockThenHome() = runTest {
        val (vm, gate) = locked()
        vm.openUnlock()
        vm.back()
        assertEquals(LockPage.LOCKED, vm.state.value.page)
        vm.back()
        assertNull(gate.lockFor.value)
        assertTrue(launched.isEmpty())
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class LockScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun lock(theme: ThemeMode = ThemeMode.DARK) = compose.setContent {
        ZeroTheme(theme) {
            LockScreen(4 * 60 * 60_000L + 12 * 60_000L, pun = 0, onGoHome = { calls += "home" }, onUnlock = {
                calls +=
                    "unlock"
            })
        }
    }

    private fun unlock(theme: ThemeMode = ThemeMode.DARK) = compose.setContent {
        ZeroTheme(theme) { UnlockScreen("IamWeakIneedThis", onSubmit = { calls += "submit:$it" }, onBack = { calls += "back" }) }
    }

    private fun wrong(theme: ThemeMode = ThemeMode.DARK) = compose.setContent {
        ZeroTheme(theme) { WrongPhraseScreen(pun = 0, onGoHome = { calls += "home" }, onTryAgain = { calls += "again" }) }
    }

    @Test
    fun f19_3_lockScreenSaysWhyAndOffersHomeAndUnlock() {
        lock()
        compose.onNodeWithText("Screen time is up for today.").assertExists()
        compose.onNodeWithText("4h 12m used. Essential apps still work.").assertExists()
        compose.onNodeWithText("That's enough phone for one day.").assertExists()
        compose.onNodeWithText("Go home").performClick()
        compose.onNodeWithText("Unlock").performClick()
        assertEquals(listOf("home", "unlock"), calls)
    }

    @Test
    fun f19_4_unlockShowsThePhraseAndWaitsForTyping() {
        unlock()
        compose.onNodeWithText("Type: IamWeakIneedThis").assertExists()
        // The title also reads "Unlock"; the button is the clickable one.
        val button = hasText("Unlock") and hasClickAction()
        compose.onNode(button).assertIsNotEnabled()
        compose.onNodeWithTag(LockTestTags.FIELD).performTextInput("IamWeakIneedThis")
        compose.onNode(button).assertIsEnabled().performClick()
        assertEquals(listOf("submit:IamWeakIneedThis"), calls)
    }

    @Test
    fun f19_5_wrongPhraseAsksIfYouReallyNeedIt() {
        wrong()
        compose.onNodeWithText("I am bored, I need a walk.").assertExists()
        compose.onNodeWithText("Do you really need this?").assertExists()
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Go home").performClick()
        assertEquals(listOf("again", "home"), calls)
    }

    @Test
    fun f19_3_lockScreenshot() {
        lock()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f19_3_lockLightScreenshot() {
        lock(ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_lockScreenshot() {
        lock()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f19_4_unlockScreenshot() {
        unlock()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_unlockScreenshot() {
        unlock()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f19_5_wrongPhraseScreenshot() {
        wrong()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_wrongPhraseScreenshot() {
        wrong(ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }
}
