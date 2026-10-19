package com.zeroos.core.data.backup

import android.content.SharedPreferences
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import kotlinx.coroutines.flow.first

/** Export and import of all settings (F13.1, F13.2). Settings contents are never logged. */
class BackupRepository(
    private val settings: SettingsRepository,
    private val diagnostics: DiagnosticsSettings,
    private val prefs: SharedPreferences,
    private val clock: Clock,
    private val logger: Logger
) {
    /** When settings were last exported, or null. */
    val lastExportMillis: Long?
        get() = prefs.getLong(KEY_LAST_EXPORT, 0L).takeIf { it > 0 }

    /** File name suggested in the save dialog (F13.1). */
    fun suggestedFileName(): String = backupFileName(clock)

    suspend fun export(output: OutputStream) {
        val backup = SettingsBackup(
            exportedAtMillis = clock.millis(),
            settings = settings.settings.first(),
            diagnostics = DiagnosticsBackup(diagnostics.isLoggingEnabled, diagnostics.minLevel.name)
        )
        output.write(SettingsBackupCodec.encode(backup).encodeToByteArray())
        prefs.edit().putLong(KEY_LAST_EXPORT, backup.exportedAtMillis).apply()
        logger.info(TAG, "settings exported")
    }

    fun read(input: InputStream): BackupReadResult = SettingsBackupCodec.decode(input.readBytes().decodeToString())

    /** Replaces all settings with [backup] (after the user confirmed, F13.2), keeping every value in range (D-059). */
    suspend fun restore(backup: SettingsBackup) {
        val imported = backup.settings.withinAllowedRanges()
        if (imported != backup.settings) logger.warn(TAG, "backup had values outside the allowed ranges; corrected")
        settings.update("settings imported (schema ${backup.schemaVersion})") { imported }
        diagnostics.isLoggingEnabled = backup.diagnostics.loggingEnabled
        backup.diagnostics.level?.let { name -> LogLevel.entries.firstOrNull { it.name == name } }?.let { diagnostics.minLevel = it }
    }

    private companion object {
        const val TAG = "backup"
        const val KEY_LAST_EXPORT = "last_export_millis"
    }
}

/** Suggested file name for an export, e.g. "zeroos-settings-2026-10-16.json". */
fun backupFileName(clock: Clock): String =
    "zeroos-settings-${java.time.LocalDate.now(clock)}.json"
