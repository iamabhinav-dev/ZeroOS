package com.zeroos.feature.applist

import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppListSettings
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
class AppListViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val launched = mutableListOf<AppKey>()
    private val logged = mutableListOf<String>()
    private fun key(name: String) = AppKey("pkg.$name", "Main", 0)
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Gmail", "Google Maps", "Instagram", "Maps").map { LaunchableActivity(key(it), it) }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

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

    private suspend fun TestScope.viewModel(settings: LauncherSettings = LauncherSettings()): AppListViewModel {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { settings }
        val vm =
            AppListViewModel(
                AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
                SettingsRepository(store, logger),
                logger,
                SavedStateHandle()
            )
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
    fun f2_6_noMatchFlagAndExternalSearchLogging() = runTest {
        val vm = viewModel()
        vm.onQueryChange("secret plans")
        assertTrue(vm.state.first { it.query == "secret plans" && !it.loading }.showNoMatch)
        vm.onExternalSearch("web")
        assertTrue(logged.contains("external search: web"))
        assertTrue("query must never be logged (F17.8)", logged.none { it.contains("secret") })
    }
}
