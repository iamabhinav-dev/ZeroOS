package com.zeroos.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.error
import com.zeroos.core.logging.warn
import com.zeroos.core.model.settings.LauncherSettings
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope

/** Creates the settings DataStore; a corrupted file is logged and replaced with defaults instead of crashing. */
fun createSettingsDataStore(context: Context, logger: Logger, scope: CoroutineScope): DataStore<LauncherSettings> {
    val file = context.dataStoreFile(SETTINGS_FILE_NAME)
    return DataStoreFactory.create(
        serializer = SettingsSerializer,
        corruptionHandler = settingsCorruptionHandler(file, logger),
        scope = scope,
        produceFile = { file }
    )
}

/**
 * Replaces an unreadable settings [file] with defaults, but first keeps a copy of it next to it as
 * [UNREADABLE_SETTINGS_FILE_NAME], so the setup isn't lost for good (D-059). The copy is never read by this version.
 */
internal fun settingsCorruptionHandler(file: File, logger: Logger) = ReplaceFileCorruptionHandler { e ->
    logger.error(TAG, "settings file corrupted; reset to defaults", e)
    try {
        if (file.exists()) {
            file.copyTo(File(file.parentFile, UNREADABLE_SETTINGS_FILE_NAME), overwrite = true)
            logger.warn(TAG, "kept a copy of the unreadable settings file")
        }
    } catch (copyFailed: IOException) {
        logger.warn(TAG, "couldn't keep a copy of the unreadable settings file", copyFailed)
    }
    LauncherSettings()
}

const val SETTINGS_FILE_NAME = "launcher_settings.json"

/** The last settings file that couldn't be read, kept for recovery (D-059). */
const val UNREADABLE_SETTINGS_FILE_NAME = "launcher_settings.unreadable.json"

private const val TAG = "settings"
