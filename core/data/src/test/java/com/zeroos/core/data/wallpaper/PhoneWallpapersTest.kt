package com.zeroos.core.data.wallpaper

import android.graphics.Bitmap
import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.Wallpaper
import com.zeroos.core.model.settings.WallpaperScreen
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneWallpapersTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    /** Records what was set where; can reject the lock screen or fail like a real phone. */
    private class FakeSystem : WallpaperSystem {
        val set = mutableListOf<Pair<String, Set<WallpaperScreen>>>()
        var rejectLock = false
        var fail: Exception? = null

        override fun files() = listOf("rose_dark.webp", "koi_dark.webp", "rose_light.webp")

        override fun thumbnail(item: WallpaperItem, width: Int, height: Int): Bitmap = error("not used")

        override fun set(item: WallpaperItem, screens: Set<WallpaperScreen>): Set<WallpaperScreen> {
            fail?.let { throw it }
            val accepted = if (rejectLock) screens - WallpaperScreen.LOCK else screens
            set += item.id to screens
            return accepted
        }
    }

    private val system = FakeSystem()

    private suspend fun TestScope.wallpapers(initial: LauncherSettings = LauncherSettings()): Pair<PhoneWallpapers, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { initial }
        val settings = SettingsRepository(store, logger)
        return PhoneWallpapers(settings, system, UnconfinedTestDispatcher(testScheduler), logger) to settings
    }

    private suspend fun PhoneWallpapers.item(id: String) = find(id)!!

    @Test
    fun f6_8_onlyTheChosenScreenChanges() = runTest {
        val (wallpapers, settings) = wallpapers()
        val result = wallpapers.apply(wallpapers.item("rose_dark"), setOf(WallpaperScreen.HOME))
        assertEquals(setOf(WallpaperScreen.HOME), result.accepted)
        assertEquals(listOf("rose_dark" to setOf(WallpaperScreen.HOME)), system.set)
        val history = settings.settings.first().phoneWallpapers
        assertEquals("rose_dark", history.current(WallpaperScreen.HOME))
        assertNull("lock screen untouched", history.current(WallpaperScreen.LOCK))
        assertTrue("setting changed: phoneWallpaper=rose_dark on home" in logged)
    }

    @Test
    fun f6_8_bothScreensWhenAsked() = runTest {
        val (wallpapers, settings) = wallpapers()
        val result = wallpapers.apply(wallpapers.item("koi_dark"), setOf(WallpaperScreen.HOME, WallpaperScreen.LOCK))
        assertEquals(setOf(WallpaperScreen.HOME, WallpaperScreen.LOCK), result.accepted)
        val history = settings.settings.first().phoneWallpapers
        assertEquals("koi_dark", history.current(WallpaperScreen.LOCK))
    }

    @Test
    fun f6_8_rejectedLockScreenIsReported() = runTest {
        system.rejectLock = true
        val (wallpapers, settings) = wallpapers()
        val result = wallpapers.apply(wallpapers.item("koi_dark"), setOf(WallpaperScreen.HOME, WallpaperScreen.LOCK))
        assertEquals(setOf(WallpaperScreen.LOCK), result.rejected)
        assertNull(settings.settings.first().phoneWallpapers.current(WallpaperScreen.LOCK))
        assertTrue("wallpaper koi_dark not accepted on lock" in logged)
    }

    @Test
    fun f6_8_failureChangesNothing() = runTest {
        system.fail = IOException("broken file")
        val (wallpapers, settings) = wallpapers()
        val result = wallpapers.apply(wallpapers.item("rose_dark"), setOf(WallpaperScreen.HOME))
        assertTrue(result.failed)
        assertEquals(LauncherSettings(), settings.settings.first())
        assertTrue("wallpaper rose_dark could not be set" in logged)
    }

    @Test
    fun d038_settingHomeShowsItBehindZeroOs() = runTest {
        val (wallpapers, settings) = wallpapers(LauncherSettings(appearance = AppearanceSettings(wallpaper = Wallpaper.BLACK)))
        assertTrue(wallpapers.apply(wallpapers.item("rose_dark"), setOf(WallpaperScreen.HOME)).homeBackgroundSwitched)
        assertEquals(Wallpaper.SYSTEM, settings.settings.first().appearance.wallpaper)

        val lockOnly = wallpapers.apply(wallpapers.item("koi_dark"), setOf(WallpaperScreen.LOCK))
        assertFalse(lockOnly.homeBackgroundSwitched)
    }

    @Test
    fun f6_8_restorePreviousOnOneScreen() = runTest {
        val (wallpapers, settings) = wallpapers()
        wallpapers.apply(wallpapers.item("rose_dark"), setOf(WallpaperScreen.HOME, WallpaperScreen.LOCK))
        wallpapers.apply(wallpapers.item("koi_dark"), setOf(WallpaperScreen.HOME))
        system.set.clear()
        val previous = settings.settings.first().phoneWallpapers.previous(WallpaperScreen.HOME)!!
        wallpapers.restorePrevious(WallpaperScreen.HOME, previous)
        assertEquals(listOf("rose_dark" to setOf(WallpaperScreen.HOME)), system.set)
        assertEquals(listOf("rose_dark", "koi_dark"), settings.settings.first().phoneWallpapers.home)
    }

    @Test
    fun f6_8_restoreOfARemovedImageIsReported() = runTest {
        val (wallpapers, _) = wallpapers()
        assertNull(wallpapers.restorePrevious(WallpaperScreen.HOME, "gone_dark"))
        assertTrue(system.set.isEmpty())
    }

    @Test
    fun f6_8_galleryFollowsTheme() = runTest {
        val (wallpapers, _) = wallpapers()
        assertEquals(listOf("pure_black", "koi_dark", "rose_dark"), wallpapers.gallery(WallpaperTone.DARK).map { it.id })
        assertEquals(listOf("pure_white", "rose_light"), wallpapers.gallery(WallpaperTone.LIGHT).map { it.id })
    }
}
