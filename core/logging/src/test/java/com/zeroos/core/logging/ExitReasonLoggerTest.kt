package com.zeroos.core.logging

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExitReasonLoggerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private class RecordingLogger : Logger {
        val lines = mutableListOf<Pair<LogLevel, String>>()

        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            lines += level to message
        }
    }

    private val exits = mutableListOf<ExitRecord>()
    private val logger = RecordingLogger()
    private val lastSeen by lazy { File(temp.root, "exit/last_seen.txt") }
    private val exitLogger by lazy { ExitReasonLogger({ exits.toList() }, lastSeen, logger) }

    @Test
    fun f17_6_logsEachNewExitOnceOldestFirst() {
        exits += ExitRecord(200, ExitReason.KILLED_BY_SYSTEM, "signal 9")
        exits += ExitRecord(100, ExitReason.CRASH, null)

        assertEquals(listOf(100L, 200L), exitLogger.logNewExits().map { it.timeMillis })
        assertEquals(
            listOf(
                LogLevel.WARN to "previous stop: crash",
                LogLevel.WARN to "previous stop: killed by the system or phone maker (signal 9)"
            ),
            logger.lines
        )
        assertTrue(exitLogger.logNewExits().isEmpty())

        exits += ExitRecord(300, ExitReason.SELF, null)
        exitLogger.logNewExits()
        assertEquals(LogLevel.INFO to "previous stop: exited normally", logger.lines.last())
    }

    @Test
    fun f17_12_sourceFailureIsSwallowed() {
        val failing = ExitReasonLogger({ error("no access") }, lastSeen, logger)
        assertTrue(failing.logNewExits().isEmpty())
    }
}
