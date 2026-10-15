package com.zeroos.core.logging

/** One entry read back from the log files for the Diagnostics screen (F17.9). */
data class StoredLogEntry(
    val time: String,
    val level: LogLevel?,
    val tag: String,
    val message: String,
    /** Indented continuation lines, e.g. a stack trace. */
    val details: List<String>
)

/**
 * Parses lines in file order (oldest first) written by [LogEntry.format]. Lines that don't start a new
 * entry are attached to the previous one. Returns entries newest first.
 */
fun parseLogLines(lines: List<String>): List<StoredLogEntry> {
    val entries = mutableListOf<StoredLogEntry>()
    lines.forEach { line ->
        val match = HEADER.matchEntire(line)
        when {
            match != null -> {
                val groups = match.groupValues
                entries += StoredLogEntry(
                    time = groups[1],
                    level = LogLevel.entries.firstOrNull { it.letter == groups[2].single() },
                    tag = groups[3],
                    message = groups[4],
                    details = emptyList()
                )
            }

            entries.isNotEmpty() -> entries[entries.lastIndex] = entries.last().let { it.copy(details = it.details + line.trim()) }
        }
    }
    return entries.asReversed()
}

/** `2026-10-15T10:42:03.123+05:30 I/home: shown` → time, level letter, tag, message. */
private val HEADER = Regex("""^(\S+) ([DIWE])/([^:]+): (.*)$""")
