package com.zeroos.core.logging

import java.io.File

/** Decides whether to show "ZeroOS crashed last time" on home, once per new crash report (F17.10). */
class CrashNotice(private val store: LogFileStore, private val settings: DiagnosticsSettings) {
    /** The newest crash report if I haven't seen it yet, otherwise null. */
    fun unseenReport(): File? = store.crashReports().firstOrNull()?.takeIf { it.name != settings.lastSeenCrashReport }

    fun markSeen(report: File) {
        settings.lastSeenCrashReport = report.name
    }
}
