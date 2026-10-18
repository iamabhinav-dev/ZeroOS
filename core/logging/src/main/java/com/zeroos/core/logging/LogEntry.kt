package com.zeroos.core.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One log line (F17.2). [stackTrace] is set for entries logged with a throwable. */
data class LogEntry(
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
) {
    /** `2026-10-15T10:42:03.123+01:00 I/home: shown`, followed by indented stack trace lines. */
    fun format(zone: ZoneId): String = buildString {
        append(TIME_FORMAT.format(Instant.ofEpochMilli(timeMillis).atZone(zone)))
        append(' ').append(level.letter).append('/').append(tag).append(": ").append(message)
        stackTrace?.lineSequence()?.filter { it.isNotBlank() }?.forEach { append("\n    ").append(it.trim()) }
    }

    companion object {
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

        fun stackTraceOf(throwable: Throwable): String =
            StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
    }
}
