package com.zeroos.feature.settings

import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.settings.FavoritesRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesEditorViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun key(name: String) = AppKey("pkg.$name", "Main", 0)
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Camera", "Gmail", "Maps", "Phone").map { LaunchableActivity(key(it), it) }

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun launch(key: AppKey) = Unit
    }
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f3_1_f3_2_editsFavorites() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData {
            LauncherSettings(
                favorites = listOf(key("Phone").asString(), key("Maps").asString()),
                renamedApps = mapOf(key("Maps").asString() to "Navigate")
            )
        }
        val settings = SettingsRepository(store, logger)
        val vm =
            FavoritesEditorViewModel(
                AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
                settings,
                FavoritesRepository(settings),
                SavedStateHandle()
            )
        backgroundScope.launch { vm.state.collect {} }

        val initial = vm.state.first { it.favorites.isNotEmpty() }
        assertEquals(listOf("Phone", "Navigate"), initial.favorites.map { it.label })
        assertEquals(listOf("Camera", "Gmail"), initial.candidates.map { it.label })
        assertEquals(6, initial.max)

        vm.onQueryChange("gm")
        assertEquals(listOf("Gmail"), vm.state.first { it.query == "gm" }.candidates.map { it.label })

        vm.add(initial.candidates.last())
        vm.move(2, 0)
        vm.remove(initial.favorites.first())
        assertEquals(
            listOf("Gmail", "Navigate"),
            vm.state.first {
                it.favorites.size == 2 && it.favorites[0].label == "Gmail"
            }.favorites.map { it.label }
        )
    }
}
