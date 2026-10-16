package com.zeroos.core.data.settings

import com.zeroos.core.logging.redacted
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings

/** Longest custom app name (wireframe E2). */
const val MAX_CUSTOM_NAME_LENGTH = 40

/** Sets a custom name (F5.4), or removes it when [name] is blank (F5.5 "Reset to original"). */
fun LauncherSettings.renaming(key: AppKey, name: String?): LauncherSettings {
    val id = key.asString()
    val clean = name?.trim()?.take(MAX_CUSTOM_NAME_LENGTH)
    return copy(renamedApps = if (clean.isNullOrEmpty()) renamedApps - id else renamedApps + (id to clean))
}

/** Hides an app from the list and search (F5.1). Hidden apps also leave the favorites. */
fun LauncherSettings.hiding(key: AppKey): LauncherSettings =
    copy(hiddenApps = hiddenApps + key.asString(), favorites = favorites - key.asString())

/** Shows a hidden app again (F5.2). */
fun LauncherSettings.unhiding(key: AppKey): LauncherSettings = copy(hiddenApps = hiddenApps - key.asString())

/** Rename and hide with logging; custom names are never logged, only their length (F17.8). */
class AppCustomizationRepository(private val settings: SettingsRepository) {
    suspend fun rename(key: AppKey, name: String?) {
        val change = if (name.isNullOrBlank()) "rename reset ${key.packageName}" else "rename ${key.packageName}=${redacted(name.trim())}"
        settings.update(change) { it.renaming(key, name) }
    }

    suspend fun hide(key: AppKey) = settings.update("hidden+=${key.packageName}") { it.hiding(key) }

    suspend fun unhide(key: AppKey) = settings.update("hidden-=${key.packageName}") { it.unhiding(key) }
}
