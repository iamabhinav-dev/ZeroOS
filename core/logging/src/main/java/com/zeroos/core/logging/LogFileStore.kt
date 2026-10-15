package com.zeroos.core.logging

import java.io.File
import java.time.Clock
import java.time.Duration

/**
 * Rotating log files in private storage (F17.2, F17.3): `log-0.txt` is current; when it passes
 * [maxFileBytes] it becomes `log-1.txt`, and so on up to [maxFiles]. Files older than [retention]
 * and crash reports beyond [maxCrashReports] are deleted.
 */
class LogFileStore(
    private val directory: File,
    private val clock: Clock,
    private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
    private val maxFiles: Int = DEFAULT_MAX_FILES,
    private val retention: Duration = DEFAULT_RETENTION,
    private val maxCrashReports: Int = DEFAULT_MAX_CRASH_REPORTS
) {
    private fun logFile(index: Int) = File(directory, "log-$index.txt")

    /** Log files, newest first. */
    fun logFiles(): List<File> = (0 until maxFiles).map(::logFile).filter { it.exists() }

    /** Crash reports, newest first. */
    fun crashReports(): List<File> =
        directory.listFiles { file -> file.name.startsWith(CRASH_PREFIX) }.orEmpty().sortedByDescending { it.name }

    @Synchronized
    fun append(line: String) {
        directory.mkdirs()
        val current = logFile(0)
        if (current.exists() && current.length() + line.length + 1 > maxFileBytes) rotate()
        current.appendText(line + "\n")
    }

    @Synchronized
    fun writeCrashReport(content: String): File {
        directory.mkdirs()
        val file = File(directory, "$CRASH_PREFIX${clock.millis()}.txt")
        file.writeText(content)
        crashReports().drop(maxCrashReports).forEach { it.delete() }
        return file
    }

    /** Deletes files whose last change is older than [retention]. */
    @Synchronized
    fun deleteExpired() {
        val cutoff = clock.millis() - retention.toMillis()
        (logFiles() + crashReports()).filter { it.lastModified() < cutoff }.forEach { it.delete() }
    }

    @Synchronized
    fun clear() {
        (logFiles() + crashReports()).forEach { it.delete() }
    }

    /** All log lines in file order, oldest first (for [parseLogLines]). */
    fun readAllLinesOldestFirst(): List<String> = logFiles().asReversed().flatMap { it.readLines() }

    /** All log lines, newest first, up to [limit]. */
    fun readRecentLines(limit: Int): List<String> =
        logFiles().asSequence().flatMap { it.readLines().asReversed().asSequence() }.take(limit).toList()

    private fun rotate() {
        logFile(maxFiles - 1).delete()
        for (index in maxFiles - 2 downTo 0) {
            val file = logFile(index)
            if (file.exists()) file.renameTo(logFile(index + 1))
        }
    }

    companion object {
        const val CRASH_PREFIX = "crash-"
        const val DEFAULT_MAX_FILE_BYTES = 1_048_576L
        const val DEFAULT_MAX_FILES = 3
        const val DEFAULT_MAX_CRASH_REPORTS = 10
        val DEFAULT_RETENTION: Duration = Duration.ofDays(7)
    }
}
