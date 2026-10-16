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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
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

    private fun settingsRepository() = com.zeroos.core.data.settings.SettingsRepository(
        androidx.datastore.core.DataStoreFactory.create(
            com.zeroos.core.data.settings.SettingsSerializer,
            produceFile = { temp.newFile().apply { delete() } }
        ),
        logger
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun nfrP1_ownStrictModeViolationsAreKeptOthersDropped() {
        val ours = RuntimeException().apply {
            stackTrace =
                arrayOf(StackTraceElement("com.zeroos.core.logging.LogFileStore", "crashReports", null, 1))
        }
        val oem = RuntimeException().apply {
            stackTrace =
                arrayOf(StackTraceElement("oem.content.res.OemThemeZipFile", "isFileExist", null, 1))
        }
        assertEquals(true, isOwnCode(ours))
        assertEquals(false, isOwnCode(oem))
    }

    @Test
    fun f17_10_crashBannerShowsOncePerCrash() {
        val name = store.writeCrashReport("boom").name
        val first = LauncherViewModel(CrashNotice(store, settings), logger, Dispatchers.Unconfined, settingsRepository())
        assertEquals(name, first.crashReport.value)

        first.markCrashSeen()
        assertNull(first.crashReport.value)
        assertNull(LauncherViewModel(CrashNotice(store, settings), logger, Dispatchers.Unconfined, settingsRepository()).crashReport.value)
    }

    @Test
    fun f17_10_noBannerWithoutCrash() {
        assertNull(LauncherViewModel(CrashNotice(store, settings), logger, Dispatchers.Unconfined, settingsRepository()).crashReport.value)
    }

    @Test
    fun f17_7_logsScreenChanges() {
        LauncherViewModel(
            CrashNotice(store, settings),
            logger,
            Dispatchers.Unconfined,
            settingsRepository()
        ).onScreenShown(LauncherScreen.APP_LIST)
        assertEquals(listOf("nav: screen: app list"), logged)
    }
}
