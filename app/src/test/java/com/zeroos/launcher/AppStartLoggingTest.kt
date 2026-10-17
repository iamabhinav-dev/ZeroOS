package com.zeroos.launcher

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** F17.7: app start is logged with version, build type, Android version and device. F17.5: crash handler installed. */
@RunWith(AndroidJUnit4::class)
class AppStartLoggingTest {

    @Test
    fun f17_7_logsAppStartWithVersionAndDevice() {
        val app = ApplicationProvider.getApplicationContext<ZeroApplication>()
        app.logger.flushBlocking()
        val start = app.logStore.readRecentLines(50).single { it.contains("I/app: app start:") }
        assertTrue(start.contains("ZeroOS ${BuildConfig.VERSION_NAME}"))
        assertTrue(start.contains("Android"))
    }

    @Test
    fun f17_5_crashHandlerIsInstalled() {
        ApplicationProvider.getApplicationContext<ZeroApplication>()
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() is com.zeroos.core.logging.CrashHandler)
    }

    @Test
    fun nfrP4_logsMemoryWhenLeavingZeroOs() {
        val app = ApplicationProvider.getApplicationContext<ZeroApplication>()
        app.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        val deadline = System.nanoTime() + 5_000_000_000L
        var found = false
        while (!found && System.nanoTime() < deadline) {
            app.logger.flushBlocking()
            found = app.logStore.readRecentLines(50).any { Regex("""I/app: memory: \d+ MB""").containsMatchIn(it) }
            if (!found) Thread.sleep(20)
        }
        assertTrue(found)
    }

    @Test
    fun nfrP4_processMemoryIsReadable() {
        assertTrue(com.zeroos.launcher.diagnostics.ProcessMemoryReader().megabytes() >= 0)
    }
}
