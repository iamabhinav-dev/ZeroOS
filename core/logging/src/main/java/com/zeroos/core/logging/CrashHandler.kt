package com.zeroos.core.logging

import java.time.Clock

/**
 * Writes a crash report (thread, app/device info, stack trace, last log entries) synchronously, then
 * hands the crash to the previous handler so Android still shows its crash dialog (F17.5).
 */
class CrashHandler(
    private val logger: FileLogger,
    private val store: LogFileStore,
    private val clock: Clock,
    private val appInfo: String,
    private val previous: Thread.UncaughtExceptionHandler?,
    private val onCrash: () -> Unit = {}
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            logger.error(TAG, "uncaught exception on thread ${thread.name}", throwable)
            logger.flushBlocking()
            store.writeCrashReport(report(thread, throwable))
            onCrash()
        }
        previous?.uncaughtException(thread, throwable)
    }

    private fun report(thread: Thread, throwable: Throwable): String = buildString {
        appendLine("ZeroOS crash report")
        appendLine("time: ${LogEntry(clock.millis(), LogLevel.ERROR, TAG, "").format(clock.zone).substringBefore(' ')}")
        appendLine("thread: ${thread.name}")
        appendLine(appInfo)
        appendLine()
        appendLine(LogEntry.stackTraceOf(throwable).trimEnd())
        appendLine()
        appendLine("Last ${logger.recentEntries().size} log entries:")
        logger.recentEntries().forEach { appendLine(it.format(clock.zone)) }
    }

    companion object {
        const val TAG = "crash"

        /** Installs a [CrashHandler] in front of the current default handler. */
        fun install(logger: FileLogger, store: LogFileStore, clock: Clock, appInfo: String, onCrash: () -> Unit = {}) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(logger, store, clock, appInfo, previous, onCrash))
        }
    }
}
