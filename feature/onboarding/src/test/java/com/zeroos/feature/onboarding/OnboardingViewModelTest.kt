package com.zeroos.feature.onboarding

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun key(name: String) = AppKey("pkg.${name.lowercase()}", "Main", 0)
    private val names = listOf("Calculator", "Camera", "Clock", "Gmail", "Maps", "Messages", "Phone")
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = names.map { LaunchableActivity(key(it), it) }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) = Unit
    }
    private val system = object : OnboardingSystem {
        var default = false
        var optimized = true

        override fun isDefaultHome() = default

        override fun isBatteryOptimized() = optimized

        // "pkg.unknown" isn't installed and is left out.
        override fun suggestedPackages() = listOf("pkg.phone", "pkg.messages", "pkg.unknown", "pkg.camera", "pkg.maps")
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

    private suspend fun TestScope.viewModel(
        initial: LauncherSettings = LauncherSettings(),
        saved: SavedStateHandle = SavedStateHandle()
    ): Pair<OnboardingViewModel, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { initial }
        val settings = SettingsRepository(store, logger)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val vm = OnboardingViewModel(AppsRepository(source, logger, dispatcher), settings, system, logger, dispatcher, saved)
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.apps.isNotEmpty() && it.maxFavorites > 0 }
        return vm to settings
    }

    @Test
    fun f14_1_f14_6_pagesInOrderThenDone() = runTest {
        val (vm, settings) = viewModel()
        val seen = mutableListOf(vm.state.value.page)
        repeat(OnboardingPage.entries.size - 1) {
            vm.next()
            seen += vm.state.first { s -> s.page != seen.last() }.page
        }
        assertEquals(OnboardingPage.entries, seen)
        vm.next()
        settings.settings.first { it.onboardingCompleted }
        assertTrue("onboarding finished" in logged)
    }

    @Test
    fun f14_3_suggestionsComeFirstAndArePreselected() = runTest {
        val (vm, _) = viewModel()
        val state = vm.state.value
        assertEquals(listOf("Phone", "Messages", "Camera", "Maps", "Calculator", "Clock", "Gmail"), state.apps.map { it.label })
        assertEquals(listOf("Phone", "Messages", "Camera", "Maps").map { key(it).asString() }, state.selected)
    }

    @Test
    fun f14_3_existingFavoritesArePreselected() = runTest {
        val (vm, _) = viewModel(LauncherSettings(favorites = listOf(key("Gmail").asString())))
        assertEquals(listOf(key("Gmail").asString()), vm.state.value.selected)
    }

    @Test
    fun f14_3_f3_3_pickingIsLimitedToMaxAndSaved() = runTest {
        val (vm, settings) = viewModel(LauncherSettings(home = HomeSettings(maxFavorites = 4)))
        val apps = vm.state.value.apps.associateBy { it.label }
        vm.toggle(apps.getValue("Gmail"))
        assertEquals(4, vm.state.value.selected.size)
        vm.toggle(apps.getValue("Camera"))
        vm.toggle(apps.getValue("Gmail"))
        val expected = listOf("Phone", "Messages", "Maps", "Gmail").map { key(it).asString() }
        assertEquals(expected, vm.state.first { it.selected.size == 4 }.selected)

        repeat(OnboardingPage.FAVORITES.ordinal) { vm.next() }
        vm.state.first { it.page == OnboardingPage.FAVORITES }
        vm.next()
        settings.settings.first { it.favorites == expected }
    }

    @Test
    fun f14_5_skipKeepsSettingsAndFinishes() = runTest {
        val (vm, settings) = viewModel()
        vm.next()
        vm.skip()
        val saved = settings.settings.first { it.onboardingCompleted }
        assertTrue(saved.favorites.isEmpty())
        assertTrue("onboarding skipped at intro 2" in logged)
    }

    @Test
    fun f14_5_backStepsToThePreviousPage() = runTest {
        val (vm, _) = viewModel()
        vm.back()
        assertEquals(OnboardingPage.INTRO_1, vm.state.value.page)
        vm.next()
        vm.next()
        vm.back()
        assertEquals(OnboardingPage.INTRO_2, vm.state.first { it.page == OnboardingPage.INTRO_2 }.page)
    }

    @Test
    fun nfrR7_pageAndSelectionSurviveProcessDeath() = runTest {
        val saved = SavedStateHandle(
            mapOf(
                "onboarding_page" to OnboardingPage.FAVORITES,
                "onboarding_selected" to arrayListOf("pkg.clock/Main#0")
            )
        )
        val (vm, _) = viewModel(saved = saved)
        val state = vm.state.first { it.page == OnboardingPage.FAVORITES }
        assertEquals(listOf("pkg.clock/Main#0"), state.selected)
    }

    @Test
    fun f14_2_f14_6_statusIsReadAgainOnReturn() = runTest {
        val (vm, _) = viewModel()
        assertFalse(vm.state.value.isDefaultHome)
        assertTrue(vm.state.value.batteryOptimized)
        system.default = true
        system.optimized = false
        vm.refreshSystemStatus()
        val state = vm.state.first { it.isDefaultHome }
        assertFalse(state.batteryOptimized)
        vm.onDefaultHomeResult(true)
        assertTrue("default home request: granted" in logged)
    }
}
