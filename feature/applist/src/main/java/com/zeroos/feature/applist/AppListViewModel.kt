package com.zeroos.feature.applist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppActions
import com.zeroos.core.data.apps.AppSearchIndex
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.settings.AddFavoriteResult
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.LauncherSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the app list shows (F2, wireframe B). */
data class AppListUiState(
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
    val options: AppListSettings = AppListSettings(),
    /** True while loading the first list, so "no match" isn't shown too early. */
    val loading: Boolean = true,
    /** For the long-press menu (F2.8): favorites, renamed apps and the favorites limit. */
    val favoriteKeys: Set<String> = emptySet(),
    val renamedKeys: Set<String> = emptySet(),
    val maxFavorites: Int = 0
) {
    val showNoMatch: Boolean get() = !loading && query.isNotBlank() && results.isEmpty()
}

/** App list with search (Step 2.4). The query is never logged (F17.8). */
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppsRepository,
    settingsRepository: SettingsRepository,
    private val actions: AppActions,
    private val logger: Logger,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private class Indexed(val search: AppSearchIndex, val settings: LauncherSettings)

    private val index = combine(repository.apps, settingsRepository.settings) { apps, settings ->
        Indexed(AppSearchIndex(visibleApps(apps, settings)), settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val state: StateFlow<AppListUiState> = combine(index, savedState.getStateFlow(KEY_QUERY, "")) { indexed, query ->
        if (indexed == null) {
            AppListUiState(query = query)
        } else {
            val s = indexed.settings
            AppListUiState(
                query = query,
                results = indexed.search.search(query),
                options = s.appList,
                loading = false,
                favoriteKeys = s.favorites.toSet(),
                renamedKeys = s.renamedApps.keys,
                maxFavorites = s.home.maxFavorites
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppListUiState())

    /** Adds or removes a favorite (F3.1); [onFull] is called with the limit if there's no room (F3.3). */
    fun toggleFavorite(app: AppEntry, onFull: (Int) -> Unit) {
        viewModelScope.launch {
            if (app.key.asString() in state.value.favoriteKeys) {
                actions.removeFavorite(app)
            } else if (actions.addFavorite(app) == AddFavoriteResult.FULL) {
                onFull(state.value.maxFavorites)
            }
        }
    }

    fun rename(app: AppEntry, name: String?) {
        viewModelScope.launch { actions.rename(app.key, name) }
    }

    fun hide(app: AppEntry) {
        viewModelScope.launch { actions.hide(app) }
    }

    fun openAppInfo(app: AppEntry) {
        actions.openAppInfo(app)
    }

    /**
     * Updates the search. With "open single match automatically" on, a query matching exactly one app opens it
     * (F2.5); returns true when that happened so the list can close.
     */
    fun onQueryChange(query: String): Boolean {
        savedState[KEY_QUERY] = query
        val single = index.value
            ?.takeIf { it.settings.appList.autoLaunchSingleMatch && query.isNotBlank() }
            ?.search?.search(query)?.singleOrNull()
        return single != null && launch(single)
    }

    /** Returns true if the app started. */
    fun launch(app: AppEntry): Boolean = repository.launch(app.key)

    fun clearQuery() {
        savedState[KEY_QUERY] = ""
    }

    /** Web search, Play Store search or the uninstall dialog opened (F2.6, F2.8); queries aren't logged. */
    fun onExternalSearch(target: String) = logger.info(TAG, "opened: $target")

    fun onExternalSearchFailed(target: String, e: Exception) = logger.warn(TAG, "could not open $target", e)

    private companion object {
        const val TAG = "applist"
        const val KEY_QUERY = "query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Letters for the fast scroller (F2.7): first letter of each visible app, in list order. */
fun scrollerLetters(apps: List<AppEntry>): List<Char> =
    apps.mapNotNull { AppSearchIndex.normalize(it.label).firstOrNull()?.uppercaseChar() }.distinct()
