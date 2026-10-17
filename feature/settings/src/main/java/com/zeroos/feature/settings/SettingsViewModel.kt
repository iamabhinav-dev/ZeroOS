package com.zeroos.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppSearchIndex
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.GestureSettings
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.LauncherSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Swipe gestures that open a chosen app (F4.2, F4.3). */
enum class SwipeSide { LEFT, RIGHT }

/** Settings shown on the Home, App list and Gestures pages (wireframes F2–F4). */
data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    /** Hidden apps that are still installed (wireframe F3 "Hidden apps (2)"). */
    val hiddenCount: Int = 0,
    /** Apps chosen for side swipes, or null when off or no longer installed. */
    val swipeLeftApp: AppEntry? = null,
    val swipeRightApp: AppEntry? = null,
    /** Apps for the swipe-app picker, filtered by [query]. */
    val pickerApps: List<AppEntry> = emptyList(),
    val query: String = "",
    /** Accessibility is switched on for ZeroOS in Android's settings (Step 4.2). */
    val accessibilityEnabled: Boolean = false,
    /** ZeroOS's Accessibility service is actually running; on but not running = the phone stopped it. */
    val accessibilityRunning: Boolean = false
)

/**
 * Settings structure (Step 2.11). Every change is logged by [SettingsRepository] (F17.7); app choices are
 * logged as "set"/"off" only.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val settings: SettingsRepository,
    private val logger: Logger,
    private val savedState: SavedStateHandle,
    /** Used by the Gestures page for the Accessibility explanation (Step 4.1). */
    val permissions: SpecialPermissions,
    systemActions: SystemActions
) : ViewModel() {

    private val accessibilityEnabled = MutableStateFlow(permissions.isGranted(SpecialPermission.ACCESSIBILITY))

    val state: StateFlow<SettingsUiState> = combine(
        appsRepository.apps,
        settings.settings,
        savedState.getStateFlow(KEY_QUERY, ""),
        accessibilityEnabled,
        systemActions.running
    ) { apps, current, query, enabled, running ->
        val visible = visibleApps(apps, current)
        val byKey = visible.associateBy { it.key.asString() }
        SettingsUiState(
            settings = current,
            hiddenCount = apps.count { it.key.asString() in current.hiddenApps },
            swipeLeftApp = current.gestures.swipeLeftApp?.let(byKey::get),
            swipeRightApp = current.gestures.swipeRightApp?.let(byKey::get),
            pickerApps = AppSearchIndex(visible).search(query),
            query = query,
            accessibilityEnabled = enabled,
            accessibilityRunning = running
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    private val current get() = state.value.settings

    fun onPageShown(page: SettingsPage) = logger.info(TAG, "settings page: ${page.logName}")

    /** On/off rows on the Home, App list and Gestures pages. */
    fun setToggle(toggle: SettingToggle, on: Boolean) =
        update("${toggle.logName}=$on") { toggle.apply(it, on) }

    /** Rows that step through their options when tapped. */
    fun cycle(option: SettingCycle) {
        // The log shows the value expected from what's on screen; the store applies the step to its own latest value.
        update("${option.logName}=${option.value(option.next(current))}", option.next)
    }

    // Gestures (wireframe F4)

    fun onPickerQueryChange(query: String) {
        savedState[KEY_QUERY] = query
    }

    /** [app] null turns the swipe off (F4.6, D-026). */
    fun setSwipeApp(side: SwipeSide, app: AppEntry?) {
        savedState[KEY_QUERY] = ""
        val key = app?.key?.asString()
        val value = if (key == null) "off" else "set"
        when (side) {
            SwipeSide.LEFT -> gestures("swipeLeft=$value") { it.copy(swipeLeftApp = key) }
            SwipeSide.RIGHT -> gestures("swipeRight=$value") { it.copy(swipeRightApp = key) }
        }
    }

    /** Checked again whenever the Gestures page comes back to the front (Step 4.2). */
    fun refreshAccessibility() {
        accessibilityEnabled.value = permissions.isGranted(SpecialPermission.ACCESSIBILITY)
    }

    /** Double-tap to lock (F4.5); turning it on needs Accessibility, asked for by the page first. */
    fun setDoubleTapLock(on: Boolean) = gestures("doubleTapLock=$on") { it.copy(doubleTapLock = on) }

    /** Result of the Accessibility explanation (Step 4.1); granted also turns double-tap on. */
    fun onAccessibilityResult(granted: Boolean) {
        permissions.record(SpecialPermission.ACCESSIBILITY, granted)
        refreshAccessibility()
        if (granted) setDoubleTapLock(true)
    }

    private fun gestures(change: String, transform: (GestureSettings) -> GestureSettings) =
        update("gestures.$change") { it.copy(gestures = transform(it.gestures)) }

    private fun update(change: String, transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { settings.update(change, transform) }
    }

    private companion object {
        const val TAG = "settings"
        const val KEY_QUERY = "settings_picker_query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** On/off settings (F1.3, F1.8, F2.3, F2.5, F2.7). */
enum class SettingToggle(val logName: String, val apply: (LauncherSettings, Boolean) -> LauncherSettings) {
    SHOW_BATTERY("home.showBattery", { s, on -> s.copy(home = s.home.copy(showBattery = on)) }),
    SHOW_NEXT_ALARM("home.showNextAlarm", { s, on -> s.copy(home = s.home.copy(showNextAlarm = on)) }),
    KEYBOARD_AUTO_OPEN("appList.keyboardAutoOpen", { s, on -> s.copy(appList = s.appList.copy(keyboardAutoOpen = on)) }),
    AUTO_LAUNCH_SINGLE_MATCH("appList.autoLaunchSingleMatch", { s, on -> s.copy(appList = s.appList.copy(autoLaunchSingleMatch = on)) }),
    FAST_SCROLLER("appList.fastScroller", { s, on -> s.copy(appList = s.appList.copy(fastScroller = on)) })
}

/** Settings that step through options when tapped (F1.7, F3.3, F5.3). */
enum class SettingCycle(
    val logName: String,
    val next: (LauncherSettings) -> LauncherSettings,
    val value: (LauncherSettings) -> Any
) {
    /** 1 → 10 → 1 (F3.3). */
    MAX_FAVORITES(
        "home.maxFavorites",
        { s -> s.copy(home = s.home.copy(maxFavorites = s.home.maxFavorites % FAVORITES_LIMIT + 1)) },
        { it.home.maxFavorites }
    ),
    ALIGNMENT("home.alignment", { s -> s.copy(home = s.home.copy(alignment = s.home.alignment.next())) }, { it.home.alignment }),
    HIDDEN_APPS_CONFIRMATION(
        "appList.hiddenAppsConfirmation",
        { s -> s.copy(appList = s.appList.copy(hiddenAppsConfirmation = s.appList.hiddenAppsConfirmation.next())) },
        { it.appList.hiddenAppsConfirmation }
    )
}

/** F3.3: 1–10 favorites. */
private const val FAVORITES_LIMIT = 10

private fun Alignment.next() = Alignment.entries[(ordinal + 1) % Alignment.entries.size]

private fun HiddenAppsConfirmation.next() = HiddenAppsConfirmation.entries[(ordinal + 1) % HiddenAppsConfirmation.entries.size]
