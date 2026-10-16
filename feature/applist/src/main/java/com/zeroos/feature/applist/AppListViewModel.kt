package com.zeroos.feature.applist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppSearchIndex
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
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

/** What the app list shows (F2, wireframe B). */
data class AppListUiState(
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
    val options: AppListSettings = AppListSettings(),
    /** True while loading the first list, so "no match" isn't shown too early. */
    val loading: Boolean = true
) {
    val showNoMatch: Boolean get() = !loading && query.isNotBlank() && results.isEmpty()
}

/** App list with search (Step 2.4). The query is never logged (F17.8). */
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppsRepository,
    settingsRepository: SettingsRepository,
    private val logger: Logger,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val index = combine(repository.apps, settingsRepository.settings) { apps, settings ->
        AppSearchIndex(visibleApps(apps, settings)) to settings.appList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val state: StateFlow<AppListUiState> = combine(index, savedState.getStateFlow(KEY_QUERY, "")) { indexed, query ->
        if (indexed == null) {
            AppListUiState(query = query)
        } else {
            AppListUiState(query, indexed.first.search(query), indexed.second, loading = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppListUiState())

    /**
     * Updates the search. With "open single match automatically" on, a query matching exactly one app opens it
     * (F2.5); returns true when that happened so the list can close.
     */
    fun onQueryChange(query: String): Boolean {
        savedState[KEY_QUERY] = query
        val single = index.value
            ?.takeIf { it.second.autoLaunchSingleMatch && query.isNotBlank() }
            ?.first?.search(query)?.singleOrNull()
        return single != null && launch(single)
    }

    /** Returns true if the app started. */
    fun launch(app: AppEntry): Boolean = repository.launch(app.key)

    fun clearQuery() {
        savedState[KEY_QUERY] = ""
    }

    /** Web or Play Store search opened (F2.6); the query itself isn't logged. */
    fun onExternalSearch(target: String) = logger.info(TAG, "external search: $target")

    fun onExternalSearchFailed(target: String, e: Exception) = logger.warn(TAG, "could not open $target search", e)

    private companion object {
        const val TAG = "applist"
        const val KEY_QUERY = "query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Letters for the fast scroller (F2.7): first letter of each visible app, in list order. */
fun scrollerLetters(apps: List<AppEntry>): List<Char> =
    apps.mapNotNull { AppSearchIndex.normalize(it.label).firstOrNull()?.uppercaseChar() }.distinct()
