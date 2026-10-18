package com.zeroos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppActions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home screen state (Step 2.3): time, favorites, battery and next alarm. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    timeTicks: TimeTicks,
    settingsRepository: SettingsRepository,
    batteryLevel: BatteryLevel,
    nextAlarm: NextAlarm,
    private val actions: AppActions,
    private val logger: Logger,
    private val homeApps: HomeApps
) : ViewModel() {

    private val saver = homeApps.saver(viewModelScope)

    /** Minute ticks, or second ticks while the clock shows seconds (F6.7). */
    private val time = settingsRepository.settings.map { it.appearance.showSeconds }.distinctUntilChanged()
        .flatMapLatest { seconds -> if (seconds) timeTicks.secondTicks else timeTicks.ticks }

    private val settingsAndFocus = combine(settingsRepository.settings, homeApps.focus) { s, f -> s to f }

    val state: StateFlow<HomeUiState> = combine(
        time,
        settingsAndFocus,
        // Favorites from last time first, so they show at once on a cold start (NFR-P2).
        homeApps.snapshots(),
        batteryLevel.status.onStart { emit(null) },
        nextAlarm.time
    ) { time, (settings, focus), apps, battery, alarm ->
        saver.onShown(apps, settings)
        homeUiState(time, settings, apps.apps, battery, alarm).withFocus(settings.focus, focus)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState(timeTicks.now()))

    fun launch(app: AppEntry) {
        homeApps.launch(app)
    }

    /** Held "focus · 23m left" for 3 s (F9.5). */
    fun endFocus() {
        viewModelScope.launch { homeApps.endFocus() }
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
