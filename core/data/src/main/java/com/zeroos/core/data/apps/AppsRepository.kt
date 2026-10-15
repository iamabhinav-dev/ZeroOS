package com.zeroos.core.data.apps

import android.content.ActivityNotFoundException
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Installed, launchable apps in every profile, sorted alphabetically, updated live on
 * install/uninstall/update (Step 1.2; F2.2, F2.9, F2.10).
 */
class AppsRepository(
    private val source: LauncherAppsSource,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher,
    private val locale: () -> Locale = Locale::getDefault
) {

    /**
     * Emits the full sorted list now and again after every package change. Bursts of changes
     * (e.g. an app update) collapse into one reload.
     */
    val apps: Flow<List<AppEntry>> = callbackFlow {
        trySend(Unit)
        val handle = source.registerChangeListener { packageName ->
            logger.info(TAG, "package changed: $packageName")
            trySend(Unit)
        }
        awaitClose { handle.unregister() }
    }
        .conflate()
        .map { load() }
        .flowOn(ioDispatcher)

    /** Starts the app. Returns false (and logs) if it can't be started, e.g. it was just uninstalled. */
    fun launch(key: AppKey): Boolean = try {
        source.launch(key)
        logger.info(TAG, "launched ${key.packageName}")
        true
    } catch (e: ActivityNotFoundException) {
        launchFailed(key, e)
    } catch (e: SecurityException) {
        launchFailed(key, e)
    } catch (e: IllegalStateException) {
        launchFailed(key, e)
    }

    private fun launchFailed(key: AppKey, e: Exception): Boolean {
        logger.warn(TAG, "could not launch ${key.packageName}", e)
        return false
    }

    private fun load(): List<AppEntry> {
        val collator = Collator.getInstance(locale()).apply { strength = Collator.SECONDARY }
        return source.launchableActivities()
            .map { AppEntry(it.key, it.label, it.profile) }
            .sortedWith(compareBy(collator) { entry: AppEntry -> entry.label }.thenBy { it.key.asString() })
    }

    private companion object {
        const val TAG = "apps"
    }
}
