package com.zeroos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppActions
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home screen state (Step 2.3): time, favorites, battery and next alarm. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    timeTicks: TimeTicks,
    settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    batteryLevel: BatteryLevel,
    nextAlarm: NextAlarm,
    private val actions: AppActions,
    private val logger: Logger
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        timeTicks.ticks,
        settingsRepository.settings,
        appsRepository.apps.onStart { emit(emptyList()) },
        batteryLevel.status.onStart { emit(null) },
        nextAlarm.time
    ) { time, settings, apps, battery, alarm -> homeUiState(time, settings, apps, battery, alarm) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState(timeTicks.now()))

    fun launch(app: AppEntry) {
        appsRepository.launch(app.key)
    }

    /** Favorite long-press menu (F2.8): every app here is already a favorite. */
    fun removeFavorite(app: AppEntry) {
        viewModelScope.launch { actions.removeFavorite(app) }
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

    /** Android's uninstall dialog, for the app's own profile (F2.8). */
    fun uninstall(app: AppEntry) {
        actions.uninstall(app)
    }

    /** Clock or date tapped but no Clock/Calendar app could open (F1.1, F1.2). */
    fun onShortcutFailed(name: String, e: Exception) = logger.warn(TAG, "could not open $name", e)

    private companion object {
        const val TAG = "home"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
