package com.zeroos.core.logging

import com.zeroos.core.testing.MutableClock
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashHandlerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val clock = MutableClock()
    private val store by lazy { LogFileStore(temp.root, clock) }
    private val logger by lazy { FileLogger(store, clock, writer = Executor { it.run() }) }

    @Test
    fun f17_5_writesReportWithStackTraceAndRecentEntriesThenDelegates() {
        var delegated: Throwable? = null
        var crashCallbacks = 0
        logger.info("home", "shown")
        val crash = IllegalStateException("boom")
        val handler = CrashHandler(logger, store, clock, "app 0.0.1 (debug)", { _, t -> delegated = t }, { crashCallbacks++ })

        handler.uncaughtException(Thread.currentThread(), crash)

        val report = store.crashReports().single().readText()
        assertTrue(report.contains("app 0.0.1 (debug)"))
        assertTrue(report.contains("java.lang.IllegalStateException: boom"))
        assertTrue(report.contains("I/home: shown"))
        assertTrue(report.contains("Last 2 log entries:"))
        assertSame(crash, delegated)
        assertEquals(1, crashCallbacks)
    }

    @Test
    fun f17_5_worksWithoutPreviousHandler() {
        CrashHandler(logger, store, clock, "info", previous = null).uncaughtException(Thread.currentThread(), RuntimeException())
        assertEquals(1, store.crashReports().size)
    }

    @Test
    fun f17_5_installPutsHandlerInFrontOfPreviousOne() {
        val original = Thread.getDefaultUncaughtExceptionHandler()
        try {
            CrashHandler.install(logger, store, clock, "info")
            assertTrue(Thread.getDefaultUncaughtExceptionHandler() is CrashHandler)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
        }
    }
}
