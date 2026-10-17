package com.zeroos.feature.applist

import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.zeroos.core.data.apps.AppActions
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.settings.AppCustomizationRepository
import com.zeroos.core.data.settings.FavoritesRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.LaunchGate
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
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.testing.MutableClock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val launched = mutableListOf<AppKey>()
    private val logged = mutableListOf<String>()
    private fun key(name: String) = AppKey("pkg.$name", "Main", 0)
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Gmail", "Google Maps", "Instagram", "Maps").map { LaunchableActivity(key(it), it) }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) {
            launched += key
        }
    }
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val clock = MutableClock(Instant.parse("2026-10-17T10:00:00Z"))
    private var usageEvents = emptyList<UsageEvent>()
    private var usageReads = 0
    private val usageSource = UsageEventSource { _, _ ->
        usageReads++
        usageEvents
    }

    private suspend fun TestScope.viewModel(settings: LauncherSettings = LauncherSettings()): AppListViewModel {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { settings }
        val settingsRepository = SettingsRepository(store, logger)
        val apps = AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler))
        val actions =
            AppActions(settingsRepository, FavoritesRepository(settingsRepository), AppCustomizationRepository(settingsRepository), apps)
        val io = UnconfinedTestDispatcher(testScheduler)
        val wellbeingState = WellbeingStateRepository(
            DataStoreFactory.create(
                WellbeingStateSerializer,
                scope = backgroundScope,
                produceFile = { temp.newFile().apply { delete() } }
            ),
            clock,
            logger,
            zone = clock::getZone
        )
        val status = WellbeingStatus(TodayUsage(usageSource, clock, io, logger, clock::getZone), wellbeingState)
        val gate = LaunchGate(apps, settingsRepository, backgroundScope, logger)
        val vm = AppListViewModel(apps, settingsRepository, actions, logger, SavedStateHandle(), gate, status)
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { !it.loading }
        return vm
    }

    @Test
    fun f5_1_f5_4_hiddenAppsRemovedAndCustomNamesUsed() {
        val apps = listOf("Gmail", "Instagram").map { AppEntry(key(it), it) }
        val settings =
            LauncherSettings(hiddenApps = setOf(key("Instagram").asString()), renamedApps = mapOf(key("Gmail").asString() to "Mail"))
        assertEquals(listOf("Mail"), visibleApps(apps, settings).map { it.label })
    }

    @Test
    fun f2_4_queryFiltersResults() = runTest {
        val vm = viewModel()
        vm.onQueryChange("gm")
        assertEquals(listOf("Gmail", "Google Maps"), vm.state.first { it.query == "gm" && !it.loading }.results.map { it.label })
    }

    @Test
    fun f2_5_singleMatchOpensOnlyWhenSettingIsOn() = runTest {
        val off = viewModel()
        assertFalse(off.onQueryChange("insta"))
        assertTrue(launched.isEmpty())

        val on = viewModel(LauncherSettings(appList = AppListSettings(autoLaunchSingleMatch = true)))
        assertFalse("two matches", on.onQueryChange("gm"))
        assertTrue(on.onQueryChange("insta"))
        assertEquals(listOf(key("Instagram")), launched)
    }

    @Test
    fun f2_8_menuActionsUpdateFavoritesNamesAndHidden() = runTest {
        val vm = viewModel(LauncherSettings(home = com.zeroos.core.model.settings.HomeSettings(maxFavorites = 1)))
        val gmail = vm.state.value.results.first { it.label == "Gmail" }
        val maps = vm.state.value.results.first { it.label == "Maps" }
        var fullAt = -1
        vm.toggleFavorite(gmail) { fullAt = it }
        assertTrue(vm.state.first { gmail.key.asString() in it.favoriteKeys }.favoriteKeys.size == 1)
        vm.toggleFavorite(maps) { fullAt = it }
        // "Full" changes no state, so wait (in real time) for the callback itself.
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (fullAt < 0) delay(10) } }
        assertEquals(1, fullAt)
        vm.toggleFavorite(gmail) { }
        vm.state.first { it.favoriteKeys.isEmpty() }

        vm.rename(gmail, "Mail")
        assertTrue(vm.state.first { it.results.any { r -> r.label == "Mail" } }.renamedKeys.contains(gmail.key.asString()))
        vm.hide(maps)
        assertTrue(vm.state.first { s -> s.results.none { it.key == maps.key } }.results.isNotEmpty())
        vm.openAppInfo(gmail)
        assertTrue(logged.any { it == "app info opened for pkg.Gmail" })
    }

    @Test
    fun f2_6_noMatchFlagAndExternalSearchLogging() = runTest {
        val vm = viewModel()
        vm.onQueryChange("secret plans")
        assertTrue(vm.state.first { it.query == "secret plans" && !it.loading }.showNoMatch)
        vm.onExternalSearch("web")
        assertTrue(logged.contains("opened: web"))
        assertTrue("query must never be logged (F17.8)", logged.none { it.contains("secret") })
    }

    @Test
    fun f8_5_minutesLeftForLimitedAppsAndRefreshedOnReturn() = runTest {
        val limits = WellbeingSettings(enabled = true, apps = mapOf("pkg.Instagram" to AppRule(dailyLimitMinutes = 30)))
        usageEvents = listOf(UsageEvent("pkg.Instagram", UsageEvent.Type.RESUMED, Instant.parse("2026-10-17T09:42:00Z").toEpochMilli()))
        val vm = viewModel(LauncherSettings(wellbeing = limits))
        assertEquals(mapOf("pkg.Instagram" to 12), vm.state.first { it.minutesLeft.isNotEmpty() }.minutesLeft)
        clock.advanceBy(Duration.ofMinutes(12))
        vm.refreshUsage()
        assertEquals(mapOf("pkg.Instagram" to 0), vm.state.first { it.minutesLeft["pkg.Instagram"] == 0 }.minutesLeft)
    }

    @Test
    fun f8_8_offReadsNoUsageAndF8_1_flaggedLaunchWaitsForStopScreen() = runTest {
        val rules = mapOf("pkg.Instagram" to AppRule(pause = true, dailyLimitMinutes = 30))
        val off = viewModel(LauncherSettings(wellbeing = WellbeingSettings(enabled = false, apps = rules)))
        assertTrue(off.state.value.minutesLeft.isEmpty())
        assertEquals(0, usageReads)

        val on = viewModel(LauncherSettings(wellbeing = WellbeingSettings(enabled = true, apps = rules)))
        val instagram = on.state.value.results.first { it.label == "Instagram" }
        launched.clear()
        assertTrue(on.launch(instagram))
        assertTrue("opened only after the stop screen", launched.isEmpty())
    }
}
