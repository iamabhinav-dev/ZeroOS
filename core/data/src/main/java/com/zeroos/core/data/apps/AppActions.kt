package com.zeroos.core.data.apps

import com.zeroos.core.data.settings.AddFavoriteResult
import com.zeroos.core.data.settings.AppCustomizationRepository
import com.zeroos.core.data.settings.FavoritesRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Actions of the app long-press menu (F2.8), shared by home and the app list so they behave the same. */
class AppActions(
    settings: SettingsRepository,
    private val favorites: FavoritesRepository,
    private val customization: AppCustomizationRepository,
    private val apps: AppsRepository
) {
    /** Keys of current favorites, to show "Add to" or "Remove from favorites". */
    val favoriteKeys: Flow<Set<String>> = settings.settings.map { it.favorites.toSet() }

    /** Original (system) labels by key, for the rename dialog's "Reset to original". */
    val customNames: Flow<Map<String, String>> = settings.settings.map { it.renamedApps }

    suspend fun addFavorite(app: AppEntry): AddFavoriteResult = favorites.add(app.key)

    suspend fun removeFavorite(app: AppEntry) = favorites.remove(app.key)

    suspend fun rename(key: AppKey, name: String?) = customization.rename(key, name)

    suspend fun hide(app: AppEntry) = customization.hide(app.key)

    fun openAppInfo(app: AppEntry): Boolean = apps.openAppInfo(app.key)
}
