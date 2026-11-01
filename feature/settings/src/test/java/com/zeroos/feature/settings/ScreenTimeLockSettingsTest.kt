package com.zeroos.feature.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.lock.LockUnlockStore
import com.zeroos.core.data.lock.ScreenTimeLock
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ScreenTimeLockSettings
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.testing.MutableClock
import com.zeroos.core.ui.ZeroTheme
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
@RunWith(AndroidJUnit4::class)
class ScreenTimeLockSettingsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Maps", "Video").map {
            LaunchableActivity(AppKey("com.example.${it.lowercase()}", "Main", 0), it)
        }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private lateinit var settings: SettingsRepository

    private suspend fun TestScope.viewModel(lock: ScreenTimeLockSettings): ScreenTimeLockSettingsViewModel {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(screenTimeLock = lock) }
        settings = SettingsRepository(store, logger)
        val stateStore = DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val screenTimeLock =
            ScreenTimeLock(settings, {
                0L
            }, LockUnlockStore(stateStore), MutableClock(Instant.parse("2026-11-01T10:00:00Z")), backgroundScope, logger) { emptySet() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm =
            ScreenTimeLockSettingsViewModel(
                settings,
                screenTimeLock,
                AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
                SpecialPermissions(context, logger)
            )
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        return vm
    }

    private suspend fun lock() = settings.settings.first().screenTimeLock

    @Test
    fun f19_6_whileOnWeakeningChangesNeedThePhrase() = runTest {
        val vm = viewModel(ScreenTimeLockSettings(enabled = true))
        assertTrue(vm.needsPhrase())
        assertFalse("no phrase", vm.confirm(LockChange.RaiseCap, null))
        assertFalse("wrong phrase", vm.confirm(LockChange.RaiseCap, "IamStrong"))
        runCurrent()
        assertEquals(240, lock().capMinutes)
        assertTrue(vm.confirm(LockChange.RaiseCap, "IamWeakIneedThis"))
        runCurrent()
        assertEquals(270, lock().capMinutes)
        assertTrue(vm.confirm(LockChange.AddApp("com.example.maps/Main#0"), "IamWeakIneedThis"))
        assertTrue(vm.confirm(LockChange.TurnOff, "IamWeakIneedThis"))
        runCurrent()
        assertEquals(listOf("com.example.maps/Main#0"), lock().essentialApps)
        assertFalse(lock().enabled)
    }

    @Test
    fun f19_6_stricterChangesNeverNeedIt() = runTest {
        val vm = viewModel(ScreenTimeLockSettings(enabled = true, capMinutes = 60, essentialApps = listOf("com.example.maps/Main#0")))
        vm.lowerCap()
        vm.lowerCap()
        vm.removeApp("com.example.maps/Main#0")
        runCurrent()
        assertEquals("never below 30 min", 30, lock().capMinutes)
        assertEquals(emptyList<String>(), lock().essentialApps)
    }

    @Test
    fun f19_1_whileOffEverythingAppliesAtOnce() = runTest {
        val vm = viewModel(ScreenTimeLockSettings())
        assertFalse(vm.needsPhrase())
        assertTrue(vm.confirm(LockChange.RaiseCap, null))
        vm.turnOn()
        runCurrent()
        assertTrue(lock().enabled)
    }

    @Test
    fun f19_9_newPhraseIsSavedButNeverLogged() = runTest {
        val vm = viewModel(ScreenTimeLockSettings(enabled = true))
        assertTrue(vm.confirm(LockChange.NewPhrase("NotTodayPhone"), "IamWeakIneedThis"))
        runCurrent()
        assertEquals("NotTodayPhone", lock().phrase)
        assertTrue(logged.none { it.contains("NotTodayPhone") || it.contains("IamWeak") })
        assertTrue(logged.any { it.contains("<redacted, 13 chars>") })
    }

    @Test
    fun f19_7_turningOnAsksForUsageAccessThenAccessibility() {
        assertEquals(
            SpecialPermission.ACCESSIBILITY,
            nextTurnOnStep(SpecialPermission.USAGE_ACCESS, granted = true, accessibilityOn = false)
        )
        assertNull("already on", nextTurnOnStep(SpecialPermission.USAGE_ACCESS, granted = true, accessibilityOn = true))
        assertNull("usage access refused: stop", nextTurnOnStep(SpecialPermission.USAGE_ACCESS, granted = false, accessibilityOn = false))
        assertNull(nextTurnOnStep(SpecialPermission.ACCESSIBILITY, granted = false, accessibilityOn = false))
        assertEquals(SpecialPermission.USAGE_ACCESS, firstMissing(usageAccess = false, accessibility = false))
        assertEquals(SpecialPermission.ACCESSIBILITY, firstMissing(usageAccess = true, accessibility = false))
        assertNull(firstMissing(usageAccess = true, accessibility = true))
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class ScreenTimeLockSettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = LockSettingsActions(
        onTurnOn = { calls += "on" },
        onTurnOff = { calls += "off" },
        onLowerCap = { calls += "lower" },
        onRaiseCap = { calls += "raise" },
        onOpenApps = { calls += "apps" },
        onPhrase = { calls += "phrase" }
    )
    private val apps = listOf("Maps", "Messages", "Video").map { AppEntry(AppKey("com.example.${it.lowercase()}", "Main", 0), it) }
    private val on = LockSettingsState(
        ScreenTimeLockSettings(enabled = true, essentialApps = listOf("com.example.maps/Main#0")),
        usedMillis = 112 * 60_000L,
        apps = apps
    )

    private fun show(state: LockSettingsState, theme: ThemeMode = ThemeMode.DARK) =
        compose.setContent { ZeroTheme(theme) { ScreenTimeLockScreen(state, actions, onBack = {}) } }

    @Test
    fun f19_1_l1RowsWhileOn() {
        show(on)
        compose.onNodeWithText("Lock after").assertExists()
        compose.onNodeWithText("4h").assertExists()
        compose.onNodeWithText("1 app").performClick()
        compose.onNodeWithText("IamWeakIneedThis").performClick()
        compose.onNodeWithText("Today: 1h 52m of 4h").assertExists()
        compose.onNodeWithTag(SettingsTestTags.LOCK_RAISE).performClick()
        compose.onNodeWithText("Turn off").performClick()
        assertEquals(listOf("apps", "phrase", "raise", "off"), calls)
    }

    @Test
    fun f19_8_saysPlainlyItIsFrictionNotAnUnbreakableLock() {
        show(on.copy(accessibilityOn = false))
        compose.onNodeWithText("so this is strong friction, not an unbreakable lock", substring = true).assertExists()
        compose.onNodeWithText("Accessibility is off, so only ZeroOS's own home and app list are locked.").assertExists()
    }

    @Test
    fun f19_1_offShowsTurnOn() {
        show(LockSettingsState())
        compose.onNodeWithText("Turn on").performClick()
        compose.onNodeWithText("Today", substring = true).assertDoesNotExist()
        assertEquals(listOf("on"), calls)
    }

    @Test
    fun f19_1_capArrowsStopAtTheEnds() {
        show(on.copy(settings = on.settings.copy(capMinutes = 480)))
        compose.onNodeWithTag(SettingsTestTags.LOCK_RAISE).assertIsNotEnabled()
    }

    @Test
    fun f19_3_essentialAppsPickerTogglesApps() {
        compose.setContent {
            ZeroTheme { EssentialAppsScreen(on, {}, onToggle = { key, add -> calls += "$key:$add" }, onBack = {}) }
        }
        compose.onNodeWithText("The phone app and Android Settings always work.").assertExists()
        compose.onNodeWithText("Maps").performClick()
        compose.onNodeWithText("Video").performClick()
        assertEquals(listOf("com.example.maps/Main#0:false", "com.example.video/Main#0:true"), calls)
    }

    @Test
    fun f19_1_lockSettingsScreenshot() {
        show(on)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f19_1_lockSettingsLightScreenshot() {
        show(on, ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_lockSettingsScreenshot() {
        show(on)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f19_3_essentialAppsScreenshot() {
        compose.setContent { ZeroTheme { EssentialAppsScreen(on, {}, { _, _ -> }, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_essentialAppsScreenshot() {
        compose.setContent { ZeroTheme { EssentialAppsScreen(on, {}, { _, _ -> }, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }
}
