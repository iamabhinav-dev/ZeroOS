package com.zeroos.core.logging

import android.util.Log

/** Mirrors entries to Logcat; used in debug builds only (F17.1). The only place android.util.Log is allowed. */
object LogcatMirror : (LogEntry) -> Unit {
    override fun invoke(entry: LogEntry) {
        val tag = "ZeroOS/${entry.tag}"
        val message = entry.stackTrace?.let { "${entry.message}\n$it" } ?: entry.message
        when (entry.level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }
}
