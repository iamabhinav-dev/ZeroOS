package com.zeroos.core.data.apps

import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import com.zeroos.core.model.settings.LauncherSettings
import java.io.File
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The apps home showed last time (favorites and side-swipe apps), so a cold start can show favorites at once
 * instead of waiting for the full app list (NFR-P2). Kept in the cache folder: not a setting, not backed up, and
 * replaced as soon as the real app list has loaded. Labels are the apps' own names, never custom names (F17.8).
 */
class HomeAppsCache(private val file: File, private val logger: Logger) {

    /** Last saved apps, or null if there's no usable cache. */
    fun read(): List<AppEntry>? = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()).map { it.toEntry() } else null
    } catch (e: IOException) {
        unusable(e)
    } catch (e: SerializationException) {
        unusable(e)
    } catch (e: IllegalArgumentException) {
        unusable(e)
    }

    fun write(apps: List<AppEntry>) {
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.path + ".tmp")
            temp.writeText(json.encodeToString(serializer, apps.map(::CachedApp)))
            if (!temp.renameTo(file)) throw IOException("rename failed")
        } catch (e: IOException) {
            logger.warn(TAG, "home cache not saved", e)
        }
    }

    private fun unusable(e: Exception): List<AppEntry>? {
        logger.warn(TAG, "home cache unreadable, ignored", e)
        return null
    }

    @Serializable
    private data class CachedApp(
        val packageName: String,
        val activityName: String,
        val userSerial: Long,
        val label: String,
        val profile: String,
        val copyNumber: Int? = null,
        val canUninstall: Boolean = true
    ) {
        constructor(app: AppEntry) : this(
            app.key.packageName,
            app.key.activityName,
            app.key.userSerial,
            app.label,
            app.profile.name,
            app.copyNumber,
            app.canUninstall
        )

        fun toEntry() = AppEntry(
            AppKey(packageName, activityName, userSerial),
            label,
            ProfileType.entries.firstOrNull { it.name == profile } ?: ProfileType.MAIN,
            copyNumber,
            canUninstall
        )
    }

    private companion object {
        const val TAG = "home cache"
        val json = Json { ignoreUnknownKeys = true }
        val serializer = ListSerializer(CachedApp.serializer())
    }
}

/** The apps home needs right away: favorites it shows and the side-swipe apps (F3, F4.2, F4.3). */
fun homeAppsToCache(apps: List<AppEntry>, settings: LauncherSettings): List<AppEntry> {
    val keys = settings.favorites.take(settings.home.maxFavorites).toSet() +
        listOfNotNull(settings.gestures.swipeLeftApp, settings.gestures.swipeRightApp)
    return apps.filter { it.key.asString() in keys }
}
