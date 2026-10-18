package com.zeroos.core.logging

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogEntryTest {
    private val time = Instant.parse("2026-10-15T05:12:03.123Z").toEpochMilli()

    @Test
    fun f17_2_formatsTimestampLevelTagAndMessage() {
        val line = LogEntry(time, LogLevel.INFO, "home", "shown").format(ZoneId.of("+01:00"))
        assertEquals("2026-10-15T06:12:03.123+01:00 I/home: shown", line)
    }

    @Test
    fun f17_2_indentsStackTraceLines() {
        val entry = LogEntry(time, LogLevel.ERROR, "data", "failed", LogEntry.stackTraceOf(IllegalStateException("boom")))
        val lines = entry.format(ZoneId.of("UTC")).lines()
        assertTrue(lines[0].endsWith("E/data: failed"))
        assertEquals("    java.lang.IllegalStateException: boom", lines[1])
        assertTrue(lines.drop(2).all { it.startsWith("    at ") })
    }

    @Test
    fun f17_8_redactedKeepsOnlyLength() {
        assertEquals("<redacted, 9 chars>", redacted("Time Sink"))
    }
}
