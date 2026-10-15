package com.zeroos.core.model.settings

import kotlinx.serialization.Serializable

/**
 * All launcher settings stored in DataStore (Step 2.1). Every default comes from REQUIREMENTS.md Appendix A.
 * Stored as JSON (D-029): unknown fields are ignored and missing fields get these defaults, so older and newer
 * files both load. Bump [version] and add a migration test when a change needs more than a new default.
 */
@Serializable
data class LauncherSettings(
    val version: Int = CURRENT_VERSION,
    val onboardingCompleted: Boolean = false,
    /** Favorite apps in display order, as `AppKey.asString()` (F3). */
    val favorites: List<String> = emptyList(),
    /** Hidden apps (F5.1). */
    val hiddenApps: Set<String> = emptySet(),
    /** Custom names by app key (F5.4). Private content: never log the names (F17.8). */
    val renamedApps: Map<String, String> = emptyMap(),
    val home: HomeSettings = HomeSettings(),
    val appList: AppListSettings = AppListSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val appearance: AppearanceSettings = AppearanceSettings()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class HomeSettings(
    /** F3.3: 1–10. */
    val maxFavorites: Int = 6,
    val alignment: Alignment = Alignment.LEFT,
    val showBattery: Boolean = true,
    val showNextAlarm: Boolean = false
)

@Serializable
data class AppListSettings(
    val keyboardAutoOpen: Boolean = true,
    val autoLaunchSingleMatch: Boolean = false,
    val fastScroller: Boolean = false,
    val hiddenAppsConfirmation: HiddenAppsConfirmation = HiddenAppsConfirmation.OFF
)

@Serializable
data class GestureSettings(
    /** App key opened by swipe left, or null when off (F4.2, D-026). */
    val swipeLeftApp: String? = null,
    /** App key opened by swipe right, or null when off (F4.3, D-026). */
    val swipeRightApp: String? = null,
    val swipeDownEnabled: Boolean = true,
    val swipeDownAction: SwipeDownAction = SwipeDownAction.SEARCH
)

@Serializable
data class AppearanceSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val wallpaper: Wallpaper = Wallpaper.BLACK,
    val textSize: TextSize = TextSize.MEDIUM,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showStatusBar: Boolean = true
)

@Serializable
enum class Alignment { LEFT, CENTER, RIGHT }

@Serializable
enum class HiddenAppsConfirmation { OFF, CONFIRM, DELAY }

@Serializable
enum class SwipeDownAction { SEARCH, NOTIFICATION_SHADE }

@Serializable
enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Serializable
enum class Wallpaper { BLACK, SYSTEM }

@Serializable
enum class TextSize { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
