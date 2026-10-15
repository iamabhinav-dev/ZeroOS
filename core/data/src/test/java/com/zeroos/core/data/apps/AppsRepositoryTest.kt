package com.zeroos.core.data.apps

import app.cash.turbine.test
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import java.util.Locale
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsRepositoryTest {

    private class FakeSource : LauncherAppsSource {
        var activities = mutableListOf<LaunchableActivity>()
        var listener: ((String) -> Unit)? = null
        var unregistered = false
        val launched = mutableListOf<AppKey>()
        var failLaunch = false

        override fun launchableActivities() = activities.toList()

        override fun registerChangeListener(onChange: (String) -> Unit): LauncherAppsSource.ChangeListenerHandle {
            listener = onChange
            return LauncherAppsSource.ChangeListenerHandle { unregistered = true }
        }

        override fun launch(key: AppKey) {
            if (failLaunch) throw SecurityException("gone")
            launched += key
        }
    }

    private class RecordingLogger : Logger {
        val lines = mutableListOf<Pair<LogLevel, String>>()

        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            lines += level to message
        }
    }

    private fun app(label: String, pkg: String = label.lowercase(), serial: Long = 0, main: Boolean = true) =
        LaunchableActivity(AppKey(pkg, "$pkg.Main", serial), label, if (main) ProfileType.MAIN else ProfileType.CLONE)

    private val source = FakeSource()
    private val logger = RecordingLogger()

    private fun repository(dispatcher: TestDispatcher) = AppsRepository(source, logger, dispatcher) { Locale.ENGLISH }

    @Test
    fun f2_2_listsAppsAlphabeticallyIgnoringCase() = runTest {
        source.activities += listOf(app("maps"), app("Camera"), app("Ébène", "ebene"), app("calculator"), app("Zoom"))
        repository(StandardTestDispatcher(testScheduler)).apps.test {
            assertEquals(listOf("calculator", "Camera", "Ébène", "maps", "Zoom"), awaitItem().map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f2_10_marksAppsFromOtherProfilesAndKeepsBothCopies() = runTest {
        source.activities += listOf(app("WhatsApp", serial = 0, main = true), app("WhatsApp", serial = 10, main = false))
        repository(StandardTestDispatcher(testScheduler)).apps.test {
            val entries = awaitItem()
            assertEquals(2, entries.size)
            assertEquals(listOf(true, false), entries.map { it.isMainProfile })
            assertEquals(listOf(0L, 10L), entries.map { it.key.userSerial })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f2_9_updatesWhenAppsAreInstalledOrRemoved() = runTest {
        source.activities += app("Camera")
        repository(StandardTestDispatcher(testScheduler)).apps.test {
            assertEquals(listOf("Camera"), awaitItem().map { it.label })

            source.activities += app("Maps")
            source.listener!!("maps")
            assertEquals(listOf("Camera", "Maps"), awaitItem().map { it.label })

            source.activities.removeAll { it.label == "Camera" }
            source.listener!!("camera")
            assertEquals(listOf("Maps"), awaitItem().map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(source.unregistered)
        assertTrue(logger.lines.any { it.second == "package changed: maps" })
    }

    @Test
    fun f17_7_launchStartsAppAndLogsPackage() {
        val repository = AppsRepository(source, logger, StandardTestDispatcher())
        val key = AppKey("com.example", "com.example.Main", 0)
        assertTrue(repository.launch(key))
        assertEquals(listOf(key), source.launched)
        assertEquals(LogLevel.INFO to "launched com.example", logger.lines.last())
    }

    @Test
    fun step1_2_launchFailureOfMissingProfileIsReported() {
        source.failLaunch = false
        val failing = object : LauncherAppsSource by source {
            override fun launch(key: AppKey) = error("profile gone")
        }
        assertFalse(AppsRepository(failing, logger, StandardTestDispatcher()).launch(AppKey("a", "a.Main", 99)))
    }

    @Test
    fun f17_7_launchFailureIsLoggedAndReported() {
        source.failLaunch = true
        val repository = AppsRepository(source, logger, StandardTestDispatcher())
        assertFalse(repository.launch(AppKey("com.gone", "com.gone.Main", 0)))
        assertEquals(LogLevel.WARN to "could not launch com.gone", logger.lines.last())
    }

    @Test
    fun step1_2_appKeyStringIsStable() {
        assertEquals("com.a/com.a.Main#10", AppKey("com.a", "com.a.Main", 10).asString())
    }
}
