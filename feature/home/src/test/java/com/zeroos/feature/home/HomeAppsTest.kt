package com.zeroos.feature.home

import app.cash.turbine.test
import com.zeroos.core.data.apps.HomeAppsCache
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAppsTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val cache by lazy { HomeAppsCache(File(temp.root, "home_apps.json"), logger) }
    private fun app(name: String) = AppEntry(AppKey("com.$name", "com.$name.Main", 0), name)

    @Test
    fun nfrP2_lastFavoritesFirstThenTheRealList() = runTest {
        cache.write(listOf(app("Phone")))
        val live = MutableSharedFlow<List<AppEntry>>()
        homeAppsWithCache(cache, live, UnconfinedTestDispatcher(testScheduler)).test {
            assertEquals(HomeAppsSnapshot(listOf(app("Phone")), live = false), awaitItem())
            live.emit(listOf(app("Camera"), app("Phone")))
            assertEquals(HomeAppsSnapshot(listOf(app("Camera"), app("Phone")), live = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nfrP2_firstStartWithoutCacheShowsNothingUntilLoaded() = runTest {
        homeAppsWithCache(cache, MutableSharedFlow(), UnconfinedTestDispatcher(testScheduler)).test {
            assertEquals(HomeAppsSnapshot(emptyList(), live = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nfrP2_savesOnlyRealListsAndOnlyChanges() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val saver = HomeAppsSaver(cache, backgroundScope, io)
        val settings = LauncherSettings(favorites = listOf(app("Phone").key.asString()))

        saver.onShown(HomeAppsSnapshot(listOf(app("Phone")), live = false), settings)
        assertNull("cached apps are never written back", cache.read())

        saver.onShown(HomeAppsSnapshot(listOf(app("Camera"), app("Phone")), live = true), settings)
        assertEquals(listOf(app("Phone")), cache.read())

        File(temp.root, "home_apps.json").delete()
        saver.onShown(HomeAppsSnapshot(listOf(app("Camera"), app("Phone")), live = true), settings)
        assertFalse("unchanged: not written again", File(temp.root, "home_apps.json").exists())
    }
}
