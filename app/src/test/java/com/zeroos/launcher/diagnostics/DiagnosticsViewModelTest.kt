package com.zeroos.launcher.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.FileLogger
import com.zeroos.core.logging.LogExporter
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.error
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.testing.MutableClock
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DiagnosticsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = MutableClock()
    private val dispatcher = UnconfinedTestDispatcher()
    private val store by lazy { LogFileStore(temp.newFolder("logs"), clock) }
    private val settings by lazy {
        DiagnosticsSettings(context.getSharedPreferences("diag-test", Context.MODE_PRIVATE), LogLevel.DEBUG)
    }
    private val logger by lazy {
        FileLogger(store, clock, { settings.isLoggingEnabled }, { settings.minLevel }, writer = Executor { it.run() })
    }

    private fun viewModel() = DiagnosticsViewModel(store, settings, LogExporter(store, temp.newFolder("out"), clock), logger, dispatcher)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f17_9_showsEntriesNewestFirstAndFiltersByLevel() {
        logger.info("home", "shown")
        logger.warn("data", "slow read")
        logger.error("apps", "could not launch x", SecurityException("gone"))
        val vm = viewModel()

        val all = vm.state.value.visibleEntries.map { it.message }
        assertEquals("screen opened", all.first())
        assertTrue(all.containsAll(listOf("shown", "slow read", "could not launch x")))

        vm.setFilter(LogFilter.WARN)
        assertEquals(listOf("could not launch x", "slow read"), vm.state.value.visibleEntries.map { it.message })
        assertTrue(vm.state.value.visibleEntries.first().details.first().contains("SecurityException"))
    }

    @Test
    fun f17_9_showsCrashReportsAndLastStop() {
        store.writeCrashReport("ZeroOS crash report\nthread: main\n\njava.lang.IllegalStateException: boom\n")
        logger.warn("exit", "previous stop: killed by the system or phone maker")
        val state = viewModel().state.value
        assertEquals("java.lang.IllegalStateException: boom", state.crashReports.single().title)
        assertEquals("killed by the system or phone maker", state.lastStop)
    }

    @Test
    fun f17_11_togglesLoggingAndCyclesLevel() {
        val vm = viewModel()
        vm.setLoggingEnabled(false)
        assertFalse(settings.isLoggingEnabled)
        assertFalse(vm.state.value.loggingEnabled)

        vm.cycleLevel()
        assertEquals(LogLevel.INFO, settings.minLevel)
        repeat(3) { vm.cycleLevel() }
        assertEquals(LogLevel.DEBUG, vm.state.value.level)
    }

    @Test
    fun f17_9_clearRemovesLogsAndReports() {
        logger.info("home", "shown")
        store.writeCrashReport("x")
        val vm = viewModel()
        vm.clear()
        assertTrue(store.crashReports().isEmpty())
        assertEquals(listOf("logs cleared"), vm.state.value.entries.map { it.message })
    }

    @Test
    fun f17_9_exportProducesZipAndReportsAreReadable() = runTest(dispatcher) {
        val name = store.writeCrashReport("full report").name
        val vm = viewModel()
        assertTrue(vm.export()!!.name.endsWith(".zip"))
        assertEquals("full report", vm.readReport(name))
        assertEquals("", vm.readReport("missing.txt"))
    }
}
