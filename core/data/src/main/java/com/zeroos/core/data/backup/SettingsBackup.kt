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
    val diagnostics: DiagnosticsBackup = DiagnosticsBackup(),
    /** The ZeroOS that made it (F13.7); missing in backups from 0.0.29 and earlier. */
    val appVersionName: String? = null,
    val appVersionCode: Long? = null
) {
    companion object {
        const val FORMAT = "zeroos-settings"
        const val SCHEMA_VERSION = 1
    }
}

/** The installed ZeroOS, written into every export (F13.7). */
data class AppVersion(val name: String, val code: Long)

/** How a backup's ZeroOS compares with the installed one, for the import warning (F13.6, wireframe K1). */
enum class BackupAge { SAME, OLDER, NEWER, UNKNOWN }

fun SettingsBackup.age(installed: AppVersion?): BackupAge {
    val code = appVersionCode
    return when {
        code == null || installed == null -> BackupAge.UNKNOWN
        code < installed.code -> BackupAge.OLDER
        code > installed.code -> BackupAge.NEWER
        else -> BackupAge.SAME
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
