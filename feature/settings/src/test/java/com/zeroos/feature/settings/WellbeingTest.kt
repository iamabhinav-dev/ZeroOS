package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.DisableFriction
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.ui.ZeroTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private fun app(name: String, profile: ProfileType = ProfileType.MAIN, serial: Long = 0) =
    AppEntry(AppKey("com.$name".lowercase(), "Main", serial), name, profile)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class WellbeingScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = WellbeingActions(
        onToggle = { t, on -> calls += "${t.name}:$on" },
        onCycle = { calls += it.name },
        onOpenApps = { calls += "apps" }
    )

    private fun show(settings: WellbeingSettings, theme: ThemeMode = ThemeMode.DARK) =
        compose.setContent { ZeroTheme(theme) { WellbeingScreen(settings, actions, onBack = {}) } }

    @Test
    fun f8_8_offShowsOnlyTheSwitchAndANote() {
        show(WellbeingSettings())
        compose.onNodeWithText("Off: no pauses, limits or tracking. Your apps and limits are kept.").assertExists()
        compose.onNodeWithText("Mindful pause").assertDoesNotExist()
        compose.onNode(hasText("Wellbeing") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).performClick()
        assertEquals("ENABLED:true", calls.last())
    }

    @Test
    fun f8_9_partsAndOptionsWhenOn() {
        show(WellbeingSettings(enabled = true))
        listOf("Mindful pause", "Intention prompt", "Daily limits", "Also outside ZeroOS", "Take a break", "Puns on stop screens").forEach {
            compose.onNodeWithText(it).performClick()
        }
        listOf("Pause length", "\"5 more minutes\"", "Break after", "Wait after tapping", "Turning off", "Apps").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(
            listOf(
                "PAUSE:false", "INTENTION:true", "LIMITS:false", "OUTSIDE:true", "BREAKS:true", "PUNS:false",
                "PAUSE_SECONDS", "EXTRA_MINUTES", "BREAK_AFTER", "BREAK_WAIT", "FRICTION", "apps"
            ),
            calls
        )
        listOf("2 a day", "15 min", "hold 3 s").forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun f8_1_f8_3_appsWithARuleComeFirstAndOpenTheirSheet() {
        val state = WellbeingUiState(
            settings = WellbeingSettings(enabled = true),
            apps = listOf(
                WellbeingAppRow(app("Instagram"), AppRule(pause = true, dailyLimitMinutes = 30)),
                WellbeingAppRow(app("Chrome"), AppRule(pause = true, takeBreak = true)),
                WellbeingAppRow(app("Maps"), AppRule())
            )
        )
        compose.setContent {
            ZeroTheme {
                WellbeingAppsScreen(state, onQueryChange = {
                }, onFlag = { p, f, on -> calls += "${f.name.lowercase()} $p $on" }, onCycleLimit = {
                    calls +=
                        "limit $it"
                }, onBack = {})
            }
        }
        compose.onNodeWithText("pause · 30 min").assertExists()
        compose.onNodeWithText("pause · break").assertExists()
        compose.onNodeWithText("Instagram").performClick()
        compose.onNodeWithText("Daily limit").performClick()
        compose.onNodeWithText("Mindful pause").performClick()
        compose.onNodeWithText("Take a break").performClick()
        assertEquals(listOf("limit com.instagram", "pause com.instagram false", "break com.instagram true"), calls)
    }

    @Test
    fun f8_12_holdToTurnOff() {
        var off = false
        compose.setContent {
            ZeroTheme { com.zeroos.core.ui.HoldToConfirm("Hold to turn off", kotlin.time.Duration.parse("3s"), { off = true }) }
        }
        compose.onNodeWithText("Hold to turn off").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(off)
    }

    @Test
    fun step4_3_wellbeingScreenshot() {
        show(WellbeingSettings(enabled = true, disableFriction = DisableFriction.HOLD))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_3_wellbeingOffLightScreenshot() {
        show(WellbeingSettings(), ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WellbeingViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf(
            LaunchableActivity(app("Instagram").key, "Instagram"),
            LaunchableActivity(AppKey("com.instagram", "Main", 10), "Instagram", ProfileType.CLONE),
            LaunchableActivity(app("Maps").key, "Maps"),
            LaunchableActivity(app("Chrome").key, "Chrome")
        )

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun launch(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit
    }
    private var usage = false
    private var accessibility = false
    private val permissions = mockk<SpecialPermissions>(relaxed = true) {
        every { isGranted(SpecialPermission.USAGE_ACCESS) } answers
            { usage }
        every { isGranted(SpecialPermission.ACCESSIBILITY) } answers
            { accessibility }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.viewModel(initial: LauncherSettings = LauncherSettings()): Pair<WellbeingViewModel, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { initial }
        val settings = SettingsRepository(store, logger)
        val vm =
            WellbeingViewModel(
                AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
                settings,
                permissions,
                SavedStateHandle()
            )
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.apps.isNotEmpty() }
        return vm to settings
    }

    @Test
    fun f8_8_offByDefaultWithAppendixADefaults() = runTest {
        val (vm, _) = viewModel()
        val s = vm.state.value.settings
        assertFalse(s.enabled)
        assertEquals(5, s.pauseSeconds)
        assertEquals(2, s.extraMinutesPerDay)
        assertEquals(DisableFriction.HOLD, s.disableFriction)
        assertFalse(s.intentionPrompt)
        assertTrue(s.puns)
    }

    @Test
    fun f8_1_f8_3_rulesPerPackageWithCloneSharingIt() = runTest {
        val (vm, settings) = viewModel()
        assertEquals("one row per package", 3, vm.state.value.apps.size)
        vm.setAppFlag("com.instagram", AppFlag.PAUSE, true)
        vm.cycleAppLimit("com.instagram")
        val rule = settings.settings.first {
            it.wellbeing.apps["com.instagram"]?.dailyLimitMinutes == 30
        }.wellbeing.apps.getValue("com.instagram")
        assertEquals(AppRule(pause = true, dailyLimitMinutes = 30), rule)
        assertEquals("com.instagram", vm.state.first { it.apps.first().rule.pause }.apps.first().app.key.packageName)
    }

    @Test
    fun f8_3_limitStepsCycleBackToOff() = runTest {
        val (vm, settings) = viewModel()
        repeat(LIMIT_STEPS.size - 1) { vm.cycleAppLimit("com.maps") }
        settings.settings.first { it.wellbeing.apps["com.maps"]?.dailyLimitMinutes == 20 }
        vm.cycleAppLimit("com.maps")
        assertFalse(
            "off and no pause: rule removed",
            settings.settings.first {
                "com.maps" !in it.wellbeing.apps
            }.wellbeing.apps.containsKey("com.maps")
        )
    }

    @Test
    fun f8_10_f8_12_settingsKeptWhenOffAndCycles() = runTest {
        val (vm, settings) = viewModel(
            LauncherSettings(wellbeing = WellbeingSettings(enabled = true, apps = mapOf("com.maps" to AppRule(dailyLimitMinutes = 30))))
        )
        vm.setToggle(WellbeingToggle.ENABLED, false)
        val off = settings.settings.first { !it.wellbeing.enabled }.wellbeing
        assertEquals(30, off.apps.getValue("com.maps").dailyLimitMinutes)
        vm.cycle(WellbeingCycle.PAUSE_SECONDS)
        vm.cycle(WellbeingCycle.FRICTION)
        val cycled = settings.settings.first {
            it.wellbeing.pauseSeconds == 6 && it.wellbeing.disableFriction == DisableFriction.PAUSE
        }.wellbeing
        assertEquals(6, cycled.pauseSeconds)
    }

    @Test
    fun f8_11_withoutUsageAccessWellbeingRunsWithPauseOnly() = runTest {
        val (vm, settings) = viewModel()
        vm.onPermissionResult(SpecialPermission.USAGE_ACCESS, granted = false, turningOn = WellbeingToggle.ENABLED)
        val s = settings.settings.first { it.wellbeing.enabled }.wellbeing
        assertFalse(s.limitsEnabled)
        verify { permissions.record(SpecialPermission.USAGE_ACCESS, false) }

        usage = true
        vm.onPermissionResult(SpecialPermission.USAGE_ACCESS, granted = true, turningOn = WellbeingToggle.LIMITS)
        assertTrue(settings.settings.first { it.wellbeing.limitsEnabled }.wellbeing.limitsEnabled)
        assertTrue(vm.state.first { it.usageAccess }.usageAccess)
    }

    @Test
    fun f8_11_f8_12_whatASwitchDoes() {
        val limited = WellbeingSettings(enabled = true, apps = mapOf("com.maps" to AppRule(dailyLimitMinutes = 30)))
        assertEquals(ToggleOutcome.CONFIRM_OFF, toggleOutcome(limited, null, WellbeingToggle.ENABLED, false))
        assertEquals(ToggleOutcome.CONFIRM_OFF, toggleOutcome(limited, null, WellbeingToggle.LIMITS, false))
        assertEquals(
            ToggleOutcome.APPLY,
            toggleOutcome(limited.copy(disableFriction = DisableFriction.OFF), null, WellbeingToggle.ENABLED, false)
        )
        assertEquals(ToggleOutcome.APPLY, toggleOutcome(limited, null, WellbeingToggle.PUNS, false))
        assertEquals(ToggleOutcome.ASK, toggleOutcome(WellbeingSettings(), SpecialPermission.USAGE_ACCESS, WellbeingToggle.ENABLED, true))
        assertEquals(ToggleOutcome.APPLY, toggleOutcome(WellbeingSettings(), null, WellbeingToggle.ENABLED, true))
        assertEquals(
            "turning off never asks",
            ToggleOutcome.APPLY,
            toggleOutcome(WellbeingSettings(), SpecialPermission.ACCESSIBILITY, WellbeingToggle.BREAKS, false)
        )
    }

    @Test
    fun f8_11_f8_16_whichPermissionEachPartNeeds() {
        val s = WellbeingSettings(enabled = true)
        assertEquals(SpecialPermission.USAGE_ACCESS, neededPermission(WellbeingToggle.ENABLED, s, false, false))
        assertEquals(null, neededPermission(WellbeingToggle.ENABLED, s.copy(limitsEnabled = false), false, false))
        assertEquals(SpecialPermission.USAGE_ACCESS, neededPermission(WellbeingToggle.LIMITS, s, false, true))
        assertEquals(SpecialPermission.ACCESSIBILITY, neededPermission(WellbeingToggle.OUTSIDE, s, false, false))
        // Take a break needs both, Usage access first.
        assertEquals(SpecialPermission.USAGE_ACCESS, neededPermission(WellbeingToggle.BREAKS, s, false, false))
        assertEquals(SpecialPermission.ACCESSIBILITY, neededPermission(WellbeingToggle.BREAKS, s, true, false))
        assertEquals(null, neededPermission(WellbeingToggle.BREAKS, s, true, true))
        assertEquals(null, neededPermission(WellbeingToggle.PUNS, s, false, false))
    }

    @Test
    fun f8_16_f8_18_breakTurnsOnOnlyWithBothPermissionsAndCycles() = runTest {
        val (vm, settings) = viewModel(LauncherSettings(wellbeing = WellbeingSettings(enabled = true)))
        usage = true
        vm.onPermissionResult(SpecialPermission.USAGE_ACCESS, granted = true, turningOn = WellbeingToggle.BREAKS)
        assertFalse("Accessibility still missing", settings.settings.first().wellbeing.breaksEnabled)
        accessibility = true
        vm.onPermissionResult(SpecialPermission.ACCESSIBILITY, granted = true, turningOn = WellbeingToggle.BREAKS)
        assertTrue(settings.settings.first { it.wellbeing.breaksEnabled }.wellbeing.breaksEnabled)

        vm.cycle(WellbeingCycle.BREAK_AFTER)
        vm.cycle(WellbeingCycle.BREAK_WAIT)
        val cycled = settings.settings.first { it.wellbeing.breakAfterMinutes == 20 && it.wellbeing.breakWaitSeconds == 10 }
        assertEquals(20, cycled.wellbeing.breakAfterMinutes)
        repeat(6) { vm.cycle(WellbeingCycle.BREAK_AFTER) }
        assertEquals(15, settings.settings.first { it.wellbeing.breakAfterMinutes == 15 }.wellbeing.breakAfterMinutes)

        vm.setAppFlag("com.instagram", AppFlag.BREAK, true)
        val rule = settings.settings.first {
            it.wellbeing.apps["com.instagram"]?.takeBreak == true
        }.wellbeing.apps.getValue("com.instagram")
        assertEquals(AppRule(takeBreak = true), rule)
    }
}
