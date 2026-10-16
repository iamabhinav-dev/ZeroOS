package com.zeroos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.settings.AppCustomizationRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HiddenAppsState(
    val apps: List<AppEntry> = emptyList(),
    val confirmation: HiddenAppsConfirmation = HiddenAppsConfirmation.OFF
)

/** Hidden apps list (F5.2, wireframe E4). Custom names are shown if set. */
@HiltViewModel
class HiddenAppsViewModel @Inject constructor(
    appsRepository: AppsRepository,
    settingsRepository: SettingsRepository,
    private val customization: AppCustomizationRepository
) : ViewModel() {

    val state: StateFlow<HiddenAppsState> = combine(appsRepository.apps, settingsRepository.settings) { apps, settings ->
        val hidden = apps.filter { it.key.asString() in settings.hiddenApps }
            .map { app -> settings.renamedApps[app.key.asString()]?.let { app.copy(label = it) } ?: app }
        HiddenAppsState(hidden, settings.appList.hiddenAppsConfirmation)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HiddenAppsState())

    fun unhide(app: AppEntry) {
        viewModelScope.launch { customization.unhide(app.key) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
