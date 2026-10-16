package com.zeroos.core.data.settings

import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings

/** Result of adding a favorite (F3.1, F3.3). */
enum class AddFavoriteResult { ADDED, ALREADY_FAVORITE, FULL }

/** Adds [key] at the end, unless it's already a favorite or the maximum is reached (F3.3). */
fun LauncherSettings.addingFavorite(key: AppKey): Pair<LauncherSettings, AddFavoriteResult> {
    val id = key.asString()
    return when {
        id in favorites -> this to AddFavoriteResult.ALREADY_FAVORITE
        favorites.size >= home.maxFavorites -> this to AddFavoriteResult.FULL
        else -> copy(favorites = favorites + id) to AddFavoriteResult.ADDED
    }
}

fun LauncherSettings.removingFavorite(key: AppKey): LauncherSettings = copy(favorites = favorites - key.asString())

/** Moves the favorite at [from] to position [to] (F3.2). Out-of-range positions change nothing. */
fun LauncherSettings.movingFavorite(from: Int, to: Int): LauncherSettings {
    if (from !in favorites.indices || to !in favorites.indices || from == to) return this
    val list = favorites.toMutableList()
    list.add(to, list.removeAt(from))
    return copy(favorites = list)
}

/** Drops favorites whose app package is no longer installed (F3.4). */
fun LauncherSettings.withoutUninstalledFavorites(installedPackages: Set<String>): LauncherSettings =
    copy(favorites = favorites.filter { AppKey.packageOf(it) in installedPackages })

/** Favorite operations with logging (F17.7). Keys contain package names, which may be logged (F17.8). */
class FavoritesRepository(private val settings: SettingsRepository) {

    suspend fun add(key: AppKey): AddFavoriteResult {
        var result = AddFavoriteResult.ADDED
        settings.update("favorites+=${key.packageName}") { current -> current.addingFavorite(key).also { result = it.second }.first }
        return result
    }

    suspend fun remove(key: AppKey) = settings.update("favorites-=${key.packageName}") { it.removingFavorite(key) }

    suspend fun move(from: Int, to: Int) = settings.update("favorites moved $from->$to") { it.movingFavorite(from, to) }

    /** Removes uninstalled favorites; does nothing if [installedPackages] is empty (list not loaded yet). */
    suspend fun pruneUninstalled(installedPackages: Set<String>) {
        if (installedPackages.isEmpty()) return
        settings.updateIfChanged("favorites pruned (uninstalled apps)") { it.withoutUninstalledFavorites(installedPackages) }
    }
}
