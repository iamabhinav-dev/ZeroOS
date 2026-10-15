package com.zeroos.core.data.settings

import androidx.datastore.core.DataStore
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.coroutines.flow.Flow

/**
 * The single place settings are read and changed (Step 2.1). Every change is logged with a short
 * description (F17.7); callers must redact free text such as custom app names (F17.8).
 */
class SettingsRepository(private val dataStore: DataStore<LauncherSettings>, private val logger: Logger) {

    val settings: Flow<LauncherSettings> = dataStore.data

    /** Applies [transform] atomically. [change] is logged, e.g. "home.alignment=CENTER". */
    suspend fun update(change: String, transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.updateData(transform)
        logger.info(TAG, "setting changed: $change")
    }

    private companion object {
        const val TAG = "settings"
    }
}
