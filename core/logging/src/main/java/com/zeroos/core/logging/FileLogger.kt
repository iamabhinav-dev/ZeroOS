package com.zeroos.core.logging

import java.time.Clock
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * [Logger] that writes to [LogFileStore] on a background thread (F17.2, F17.4) and keeps the last
 * [recentCapacity] entries in memory for crash reports (F17.5). Never throws (F17.12).
 *
 * @param isEnabled diagnostics logging toggle (F17.11)
 * @param minLevel lowest level written (F17.11)
 * @param mirror optional extra output, e.g. Logcat in debug builds
 */
class FileLogger(
    private val store: LogFileStore,
    private val clock: Clock,
    private val isEnabled: () -> Boolean = { true },
    private val minLevel: () -> LogLevel = { LogLevel.INFO },
    private val mirror: (LogEntry) -> Unit = {},
    private val writer: Executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "zeroos-logger").apply { isDaemon = true }
    },
    private val recentCapacity: Int = DEFAULT_RECENT_CAPACITY
) : Logger {

    private val recent = ArrayDeque<LogEntry>()

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        runCatching {
            if (!isEnabled() || level < minLevel()) return
            val entry = LogEntry(clock.millis(), level, tag, message, throwable?.let(LogEntry::stackTraceOf))
            remember(entry)
            mirror(entry)
            val line = entry.format(clock.zone)
            writer.execute { runCatching { store.append(line) } }
        }
    }

    /** The most recent entries, oldest first. */
    fun recentEntries(): List<LogEntry> = synchronized(recent) { recent.toList() }

    /** Waits until queued entries are written. Used by the crash handler before the process dies. */
    fun flushBlocking(timeoutMillis: Long = FLUSH_TIMEOUT_MILLIS) {
        runCatching {
            val executor = writer as? ExecutorService ?: return
            executor.submit {}.get(timeoutMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun remember(entry: LogEntry) = synchronized(recent) {
        recent.addLast(entry)
        while (recent.size > recentCapacity) recent.removeFirst()
    }

    companion object {
        const val DEFAULT_RECENT_CAPACITY = 50
        private const val FLUSH_TIMEOUT_MILLIS = 1_000L
    }
}
