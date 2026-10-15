package com.zeroos.core.logging

import com.zeroos.core.testing.MutableClock
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LogFileStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val clock = MutableClock()

    private fun store(maxBytes: Long = 1_000, maxFiles: Int = 3, maxCrashes: Int = 10) =
        LogFileStore(temp.root, clock, maxFileBytes = maxBytes, maxFiles = maxFiles, maxCrashReports = maxCrashes)

    @Test
    fun f17_2_appendsLinesNewestFirstOnRead() {
        val store = store()
        store.append("one")
        store.append("two")
        assertEquals(listOf("two", "one"), store.readRecentLines(10))
    }

    @Test
    fun f17_3_rotatesWhenFileIsFullAndKeepsAtMostMaxFiles() {
        val store = store(maxBytes = 20, maxFiles = 3)
        repeat(10) { store.append("line-$it-xxxxxxx") }
        val files = store.logFiles()
        assertEquals(3, files.size)
        assertTrue(files.all { it.length() <= 20 })
        assertEquals("line-9-xxxxxxx", store.readRecentLines(1).single())
        assertEquals(3, store.readRecentLines(100).size)
    }

    @Test
    fun f17_3_deletesFilesOlderThanRetention() {
        val store = store()
        store.append("old")
        store.logFiles().single().setLastModified(clock.millis())
        clock.advanceBy(Duration.ofDays(8))
        store.deleteExpired()
        assertTrue(store.logFiles().isEmpty())
    }

    @Test
    fun f17_3_keepsFilesWithinRetention() {
        val store = store()
        store.append("recent")
        store.logFiles().single().setLastModified(clock.millis())
        clock.advanceBy(Duration.ofDays(6))
        store.deleteExpired()
        assertEquals(1, store.logFiles().size)
    }

    @Test
    fun f17_5_crashReportsAreCappedNewestFirst() {
        val store = store(maxCrashes = 2)
        repeat(3) {
            store.writeCrashReport("crash $it")
            clock.advanceBy(Duration.ofSeconds(1))
        }
        val reports = store.crashReports()
        assertEquals(2, reports.size)
        assertEquals("crash 2", reports.first().readText())
    }

    @Test
    fun f17_9_clearRemovesLogsAndCrashReports() {
        val store = store()
        store.append("x")
        store.writeCrashReport("c")
        store.clear()
        assertTrue(store.logFiles().isEmpty())
        assertFalse(store.crashReports().any())
    }
}
