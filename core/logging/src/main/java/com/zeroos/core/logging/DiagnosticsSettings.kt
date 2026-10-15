package com.zeroos.core.logging

import android.content.SharedPreferences

/**
 * Diagnostics settings (F17.11) and which crash report I've already seen (F17.10).
 * Kept in SharedPreferences because the logger reads them on every call and they're needed from the
 * first build, before DataStore arrives in Phase 2 (D-025).
 */
class DiagnosticsSettings(private val prefs: SharedPreferences, private val defaultLevel: LogLevel) {

    var isLoggingEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var minLevel: LogLevel
        get() = prefs.getString(KEY_LEVEL, null)?.let { name -> LogLevel.entries.firstOrNull { it.name == name } } ?: defaultLevel
        set(value) = prefs.edit().putString(KEY_LEVEL, value.name).apply()

    /** File name of the newest crash report already shown to me. */
    var lastSeenCrashReport: String?
        get() = prefs.getString(KEY_LAST_SEEN_CRASH, null)
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_CRASH, value).apply()

    private companion object {
        const val KEY_ENABLED = "logging_enabled"
        const val KEY_LEVEL = "logging_level"
        const val KEY_LAST_SEEN_CRASH = "last_seen_crash_report"
    }
}
