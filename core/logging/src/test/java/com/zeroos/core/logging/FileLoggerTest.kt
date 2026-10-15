package com.zeroos.core.logging

import com.zeroos.core.testing.MutableClock
import java.io.File
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileLoggerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val clock = MutableClock()
    private val direct = Executor { it.run() }

    private fun logger(
        store: LogFileStore = LogFileStore(temp.root, clock),
        enabled: Boolean = true,
        minLevel: LogLevel = LogLevel.INFO,
        mirror: (LogEntry) -> Unit = {},
        capacity: Int = 50
    ) = FileLogger(store, clock, { enabled }, { minLevel }, mirror, direct, capacity)

    @Test
    fun f17_1_writesEntriesAtOrAboveMinLevel() {
        val store = LogFileStore(temp.root, clock)
        val logger = logger(store, minLevel = LogLevel.INFO)
        logger.debug("t", "hidden")
        logger.info("t", "shown")
        logger.error("t", "bad")
        val lines = store.readRecentLines(10)
        assertEquals(2, lines.size)
        assertTrue(lines[0].endsWith("E/t: bad"))
        assertTrue(lines[1].endsWith("I/t: shown"))
    }

    @Test
    fun f17_11_writesNothingWhenDisabled() {
        val store = LogFileStore(temp.root, clock)
        logger(store, enabled = false).error("t", "x")
        assertTrue(store.readRecentLines(10).isEmpty())
    }

    @Test
    fun f17_2_includesStackTraceForThrowables() {
        val store = LogFileStore(temp.root, clock)
        logger(store).warn("t", "caught", RuntimeException("oops"))
        val content = store.logFiles().single().readText()
        assertTrue(content.contains("W/t: caught"))
        assertTrue(content.contains("java.lang.RuntimeException: oops"))
    }

    @Test
    fun f17_5_keepsOnlyTheMostRecentEntriesInMemory() {
        val logger = logger(capacity = 3)
        repeat(5) { logger.info("t", "m$it") }
        assertEquals(listOf("m2", "m3", "m4"), logger.recentEntries().map { it.message })
    }

    @Test
    fun f17_1_mirrorsEntries() {
        val mirrored = mutableListOf<LogEntry>()
        logger(mirror = { mirrored += it }).info("t", "x")
        assertEquals("x", mirrored.single().message)
    }

    @Test
    fun f17_12_neverThrowsWhenStorageFails() {
        val blocker = File(temp.root, "not-a-dir").apply { writeText("file") }
        val logger = logger(store = LogFileStore(blocker, clock), mirror = { error("mirror failure") })
        logger.error("t", "x")
        logger.info("t", "y")
    }

    @Test
    fun f17_4_backgroundWriterFlushesOnRequest() {
        val store = LogFileStore(temp.root, clock)
        val logger = FileLogger(store, clock)
        repeat(20) { logger.info("t", "m$it") }
        logger.flushBlocking()
        assertEquals(20, store.readRecentLines(100).size)
    }
}
