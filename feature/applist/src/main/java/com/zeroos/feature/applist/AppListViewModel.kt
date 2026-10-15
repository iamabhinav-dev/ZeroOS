package com.zeroos.feature.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App list state (Step 1.3): all launchable apps, alphabetical (F2.2), live-updated (F2.9). */
@HiltViewModel
class AppListViewModel @Inject constructor(private val repository: AppsRepository) : ViewModel() {
    val apps: StateFlow<List<AppEntry>> = repository.apps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Returns true if the app started. */
    fun launch(key: AppKey): Boolean = repository.launch(key)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
