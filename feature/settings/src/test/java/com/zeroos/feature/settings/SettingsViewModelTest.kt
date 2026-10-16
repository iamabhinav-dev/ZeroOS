package com.zeroos.feature.settings

import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun key(name: String) = AppKey("pkg.$name", "Main", 0)
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Camera", "Gmail", "Maps", "Phone").map { LaunchableActivity(key(it), it) }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun launch(key: AppKey) = Unit
    }
    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.viewModel(initial: LauncherSettings = LauncherSettings()): Pair<SettingsViewModel, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { initial }
        val settings = SettingsRepository(store, logger)
        val vm =
            SettingsViewModel(AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)), settings, logger, SavedStateHandle())
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.pickerApps.isNotEmpty() }
        return vm to settings
    }

    @Test
    fun f1_3_f1_7_f1_8_f3_3_homeSettings() = runTest {
        val (vm, settings) = viewModel()
        vm.cycle(SettingCycle.MAX_FAVORITES)
        vm.state.first { it.settings.home.maxFavorites == 7 }
        vm.cycle(SettingCycle.ALIGNMENT)
        vm.state.first { it.settings.home.alignment == Alignment.CENTER }
        vm.setToggle(SettingToggle.SHOW_BATTERY, false)
        vm.setToggle(SettingToggle.SHOW_NEXT_ALARM, true)
        settings.settings.first { !it.home.showBattery && it.home.showNextAlarm }
        assertTrue("setting changed: home.alignment=CENTER" in logged)
    }

    @Test
    fun f3_3_maxFavoritesWrapsFromTenToOne() = runTest {
        val (vm, settings) = viewModel(LauncherSettings(home = HomeSettings(maxFavorites = 10)))
        vm.state.first { it.settings.home.maxFavorites == 10 }
        vm.cycle(SettingCycle.MAX_FAVORITES)
        settings.settings.first { it.home.maxFavorites == 1 }
    }

    @Test
    fun f2_3_f2_5_f2_7_f5_3_appListSettings() = runTest {
        val (vm, settings) = viewModel(LauncherSettings(hiddenApps = setOf(key("Maps").asString(), "gone/x#0")))
        assertEquals(1, vm.state.first { it.settings.hiddenApps.isNotEmpty() }.hiddenCount)
        vm.setToggle(SettingToggle.KEYBOARD_AUTO_OPEN, false)
        vm.setToggle(SettingToggle.AUTO_LAUNCH_SINGLE_MATCH, true)
        vm.setToggle(SettingToggle.FAST_SCROLLER, true)
        vm.cycle(SettingCycle.HIDDEN_APPS_CONFIRMATION)
        settings.settings.first {
            !it.appList.keyboardAutoOpen && it.appList.autoLaunchSingleMatch && it.appList.fastScroller &&
                it.appList.hiddenAppsConfirmation == HiddenAppsConfirmation.CONFIRM
        }
    }

    @Test
    fun f4_2_f4_3_f4_6_swipeAppsChosenAndTurnedOff() = runTest {
        val (vm, settings) = viewModel(LauncherSettings(renamedApps = mapOf(key("Maps").asString() to "Navigate")))
        vm.onPickerQueryChange("gm")
        assertEquals(listOf("Gmail"), vm.state.first { it.query == "gm" }.pickerApps.map { it.label })
        vm.setSwipeApp(SwipeSide.LEFT, vm.state.value.pickerApps.single())
        vm.state.first { it.swipeLeftApp != null }
        vm.setSwipeApp(SwipeSide.RIGHT, vm.state.first { it.query.isEmpty() }.pickerApps.first { it.label == "Navigate" })
        val state = vm.state.first { it.swipeRightApp != null && it.swipeLeftApp != null }
        assertEquals("Gmail", state.swipeLeftApp?.label)
        assertEquals("Navigate", state.swipeRightApp?.label)

        vm.setSwipeApp(SwipeSide.LEFT, null)
        assertNull(settings.settings.first { it.gestures.swipeLeftApp == null }.gestures.swipeLeftApp)
        assertTrue("setting changed: gestures.swipeLeft=off" in logged)
        // App names are never logged (F17.8).
        assertFalse(logged.any { "Navigate" in it || "Gmail" in it })
    }

    @Test
    fun f17_7_logsPages() = runTest {
        val (vm, _) = viewModel()
        vm.onPageShown(SettingsPage.GESTURES)
        assertTrue("settings page: gestures" in logged)
    }
}
