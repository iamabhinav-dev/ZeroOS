package com.zeroos.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.logging.CrashNotice
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.testing.MutableClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store by lazy { LogFileStore(temp.newFolder("logs"), MutableClock()) }
    private val settings by lazy { DiagnosticsSettings(context.getSharedPreferences("launcher-test", Context.MODE_PRIVATE), LogLevel.INFO) }
    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += "$tag: $message"
        }
    }

    @Test
    fun f17_10_crashBannerShowsOncePerCrash() {
        val name = store.writeCrashReport("boom").name
        val first = LauncherViewModel(CrashNotice(store, settings), logger)
        assertEquals(name, first.crashReport.value)

        first.markCrashSeen()
        assertNull(first.crashReport.value)
        assertNull(LauncherViewModel(CrashNotice(store, settings), logger).crashReport.value)
    }

    @Test
    fun f17_10_noBannerWithoutCrash() {
        assertNull(LauncherViewModel(CrashNotice(store, settings), logger).crashReport.value)
    }

    @Test
    fun f17_7_logsScreenChanges() {
        LauncherViewModel(CrashNotice(store, settings), logger).onScreenShown(LauncherScreen.APP_LIST)
        assertEquals(listOf("nav: screen: app list"), logged)
    }
}
