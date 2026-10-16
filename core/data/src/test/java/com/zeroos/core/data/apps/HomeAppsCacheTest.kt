package com.zeroos.core.data.apps

import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import com.zeroos.core.model.settings.GestureSettings
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeAppsCacheTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val warnings = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            if (level == LogLevel.WARN) warnings += message
        }
    }
    private val file by lazy { File(temp.root, "cache/home_apps.json") }
    private val cache by lazy { HomeAppsCache(file, logger) }

    private fun app(name: String, serial: Long = 0) = AppEntry(AppKey("com.$name", "com.$name.Main", serial), name)

    @Test
    fun nfrP2_savedAppsComeBackUnchanged() {
        val apps = listOf(
            app("phone").copy(canUninstall = false),
            app("WhatsApp", serial = 10).copy(profile = ProfileType.CLONE, copyNumber = 2)
        )
        cache.write(apps)
        assertEquals(apps, cache.read())
    }

    @Test
    fun nfrP2_noCacheYet() {
        assertNull(cache.read())
    }

    @Test
    fun nfrP2_damagedCacheIsIgnored() {
        file.parentFile?.mkdirs()
        file.writeText("{ not json")
        assertNull(cache.read())
        assertTrue(warnings.single().startsWith("home cache unreadable"))
    }

    @Test
    fun nfrP2_cachesShownFavoritesAndSwipeApps() {
        val apps = listOf(app("a"), app("b"), app("c"), app("d"))
        val settings = LauncherSettings(
            favorites = apps.take(3).map { it.key.asString() },
            home = HomeSettings(maxFavorites = 2),
            gestures = GestureSettings(swipeLeftApp = apps[3].key.asString())
        )
        // "c" is a favorite beyond the maximum, so home doesn't show it.
        assertEquals(listOf("a", "b", "d"), homeAppsToCache(apps, settings).map { it.label })
    }
}
