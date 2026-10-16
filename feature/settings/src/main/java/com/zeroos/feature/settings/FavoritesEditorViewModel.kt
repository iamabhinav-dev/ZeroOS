package com.zeroos.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppSearchIndex
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.settings.FavoritesRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.AppEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Favorites editor and its app picker (F3, wireframe E3). */
data class FavoritesEditorState(
    val favorites: List<AppEntry> = emptyList(),
    val max: Int = 0,
    /** Apps that can still be added, filtered by [query]. */
    val candidates: List<AppEntry> = emptyList(),
    val query: String = ""
) {
    val canAdd: Boolean get() = favorites.size < max
}

@HiltViewModel
class FavoritesEditorViewModel @Inject constructor(
    appsRepository: AppsRepository,
    settingsRepository: SettingsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    val state: StateFlow<FavoritesEditorState> = combine(
        appsRepository.apps,
        settingsRepository.settings,
        savedState.getStateFlow(KEY_QUERY, "")
    ) { apps, settings, query ->
        val visible = visibleApps(apps, settings)
        val byKey = visible.associateBy { it.key.asString() }
        val favorites = settings.favorites.mapNotNull { byKey[it] }
        val others = visible.filterNot { it.key.asString() in settings.favorites }
        FavoritesEditorState(favorites, settings.home.maxFavorites, AppSearchIndex(others).search(query), query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FavoritesEditorState())

    fun onQueryChange(query: String) {
        savedState[KEY_QUERY] = query
    }

    fun add(app: AppEntry) {
        savedState[KEY_QUERY] = ""
        viewModelScope.launch { favoritesRepository.add(app.key) }
    }

    fun remove(app: AppEntry) {
        viewModelScope.launch { favoritesRepository.remove(app.key) }
    }

    fun move(from: Int, to: Int) {
        viewModelScope.launch { favoritesRepository.move(from, to) }
    }

    private companion object {
        const val KEY_QUERY = "favorites_query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
