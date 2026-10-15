package com.zeroos.feature.applist

import app.cash.turbine.test
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {
    private val launched = mutableListOf<AppKey>()
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf(
            LaunchableActivity(AppKey("b", "b.Main", 0), "Bravo", true),
            LaunchableActivity(AppKey("a", "a.Main", 0), "alpha", true)
        )

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun launch(key: AppKey) {
            launched += key
        }
    }
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f2_2_exposesSortedApps() = runTest {
        val viewModel = AppListViewModel(AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)))
        viewModel.apps.test {
            var items = awaitItem()
            if (items.isEmpty()) items = awaitItem()
            assertEquals(listOf("alpha", "Bravo"), items.map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun step1_3_launchDelegatesToRepository() {
        val viewModel = AppListViewModel(AppsRepository(source, logger, UnconfinedTestDispatcher()))
        assertTrue(viewModel.launch(AppKey("a", "a.Main", 0)))
        assertEquals(listOf(AppKey("a", "a.Main", 0)), launched)
    }
}
