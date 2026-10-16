package com.zeroos.core.data.backup

import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/** Diagnostics settings live outside DataStore (D-025) but are part of the backup (F13.1). */
@Serializable
data class DiagnosticsBackup(val loggingEnabled: Boolean = true, val level: String? = null)

/**
 * A settings backup file (F13). [schemaVersion] lets older files import with defaults for anything missing
 * (F13.5) and newer files be refused instead of silently losing settings.
 */
@Serializable
data class SettingsBackup(
    val format: String = FORMAT,
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAtMillis: Long = 0,
    val settings: LauncherSettings = LauncherSettings(),
    val diagnostics: DiagnosticsBackup = DiagnosticsBackup()
) {
    companion object {
        const val FORMAT = "zeroos-settings"
        const val SCHEMA_VERSION = 1
    }
}

/** What reading a backup file gave. */
sealed interface BackupReadResult {
    data class Ok(val backup: SettingsBackup) : BackupReadResult

    /** Not a ZeroOS settings file (wrong format or not JSON). */
    data object NotABackup : BackupReadResult

    /** Made by a newer ZeroOS; importing could lose settings. */
    data class TooNew(val version: Int) : BackupReadResult
}

object SettingsBackupCodec {
    fun encode(backup: SettingsBackup): String = SettingsSerializer.json.encodeToString(SettingsBackup.serializer(), backup)

    fun decode(text: String): BackupReadResult {
        val backup = SettingsBackupCodecJson.parse(text)
        return when {
            backup == null || backup.format != SettingsBackup.FORMAT -> BackupReadResult.NotABackup
            backup.schemaVersion > SettingsBackup.SCHEMA_VERSION -> BackupReadResult.TooNew(backup.schemaVersion)
            else -> BackupReadResult.Ok(backup)
        }
    }
}

/** Not JSON or not a backup: [SerializationException] and [IllegalArgumentException] become null. */
private object SettingsBackupCodecJson {
    fun parse(text: String): SettingsBackup? = try {
        SettingsSerializer.json.decodeFromString(SettingsBackup.serializer(), text)
    } catch (@Suppress("SwallowedException") e: SerializationException) {
        null
    } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
        null
    }
}
