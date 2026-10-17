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
    fun nfrP4_memorySamplingFollowsTheHomeScreen() {
        val app = ApplicationProvider.getApplicationContext<ZeroApplication>()
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            controller.pause().stop()
            assertTrue("waiting for the background sample", app.memorySampler.waiting)
            controller.start()
            assertTrue("back on screen: no sample", !app.memorySampler.waiting)
        }
    }

    @Test
    fun nfrP4_processMemoryIsReadable() {
        assertTrue(com.zeroos.launcher.diagnostics.ProcessMemoryReader().megabytes() >= 0)
    }
}
