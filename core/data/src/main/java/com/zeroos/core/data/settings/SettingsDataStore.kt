package com.zeroos.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.error
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.coroutines.CoroutineScope

/** Creates the settings DataStore; a corrupted file is logged and replaced with defaults instead of crashing. */
fun createSettingsDataStore(context: Context, logger: Logger, scope: CoroutineScope): DataStore<LauncherSettings> =
    DataStoreFactory.create(
        serializer = SettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { e ->
            logger.error("settings", "settings file corrupted; reset to defaults", e)
            LauncherSettings()
        },
        scope = scope,
        produceFile = { context.dataStoreFile(SETTINGS_FILE_NAME) }
    )

const val SETTINGS_FILE_NAME = "launcher_settings.json"
