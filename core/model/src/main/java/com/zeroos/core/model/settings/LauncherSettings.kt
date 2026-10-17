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
    val appearance: AppearanceSettings = AppearanceSettings(),
    /** Phone wallpapers ZeroOS has set, for "Restore previous" (F6.8). */
    val phoneWallpapers: PhoneWallpaperHistory = PhoneWallpaperHistory(),
    /** Mindful pause and daily limits (F8); off by default, settings kept while off (F8.10). */
    val wellbeing: WellbeingSettings = WellbeingSettings()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** The phone's home and lock screens, as targets for a wallpaper (F6.8). */
@Serializable
enum class WallpaperScreen { HOME, LOCK }

/**
 * Wallpapers ZeroOS set on each screen, newest first, at most [KEPT] (F6.8). Only ZeroOS's own choices: Android
 * doesn't let apps read the current wallpaper (D-038).
 */
@Serializable
data class PhoneWallpaperHistory(val home: List<String> = emptyList(), val lock: List<String> = emptyList()) {
    fun on(screen: WallpaperScreen): List<String> = if (screen == WallpaperScreen.HOME) home else lock

    /** The wallpaper ZeroOS last set on [screen], or null. */
    fun current(screen: WallpaperScreen): String? = on(screen).firstOrNull()

    /** The one before it, for "Restore previous", or null. */
    fun previous(screen: WallpaperScreen): String? = on(screen).getOrNull(1)

    /** After [id] was set on [screen]: it becomes current, the old current becomes previous. */
    fun withSet(screen: WallpaperScreen, id: String): PhoneWallpaperHistory {
        val list = (listOf(id) + on(screen).filter { it != id }).take(KEPT)
        return if (screen == WallpaperScreen.HOME) copy(home = list) else copy(lock = list)
    }

    companion object {
        const val KEPT = 2
    }
}

/** F8 settings; defaults from Appendix A. Rules are per app package, so clones share their app's rule. */
@Serializable
data class WellbeingSettings(
    /** Master switch (F8.8). */
    val enabled: Boolean = false,
    /** Mindful pause before flagged apps (F8.1, F8.9). */
    val pauseEnabled: Boolean = true,
    /** F8.1: 3–10 s. */
    val pauseSeconds: Int = 5,
    /** "Why are you opening this?" (F8.2). */
    val intentionPrompt: Boolean = false,
    /** Daily limits (F8.3–F8.6); needs Usage access. */
    val limitsEnabled: Boolean = true,
    /** "5 more minutes" per app per day (F8.4): 0–5. */
    val extraMinutesPerDay: Int = 2,
    /** Limits also outside ZeroOS, via the Accessibility service (F8.7). */
    val outsideEnforcement: Boolean = false,
    /** Random puns on the stop screens (F8.13, F8.14). */
    val puns: Boolean = true,
    /** Friction before turning Wellbeing or limits off (F8.12). */
    val disableFriction: DisableFriction = DisableFriction.HOLD,
    /** Rules by package name (F8.1, F8.3; D-041). */
    val apps: Map<String, AppRule> = emptyMap()
) {
    /** At least one daily limit is set and limits are on: turning off then needs the friction (F8.12). */
    val hasActiveLimit: Boolean get() = enabled && limitsEnabled && apps.values.any { it.dailyLimitMinutes != null }
}

/** What Wellbeing does for one app (wireframe H5a). */
@Serializable
data class AppRule(val pause: Boolean = false, val dailyLimitMinutes: Int? = null) {
    val isEmpty: Boolean get() = !pause && dailyLimitMinutes == null
}

/** F8.12. */
@Serializable
enum class DisableFriction { OFF, HOLD, PAUSE }

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
    /** Double-tap empty space on home locks the phone; needs the Accessibility service (F4.5). */
    val doubleTapLock: Boolean = false
)

@Serializable
data class AppearanceSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val wallpaper: Wallpaper = Wallpaper.BLACK,
    val textSize: TextSize = TextSize.MEDIUM,
    val showClock: Boolean = true,
    /** Clock as hh:mm:ss instead of hh:mm (F6.7, D-036). */
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showStatusBar: Boolean = true
)

@Serializable
enum class Alignment { LEFT, CENTER, RIGHT }

@Serializable
enum class HiddenAppsConfirmation { OFF, CONFIRM, DELAY }

@Serializable
enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Serializable
enum class Wallpaper { BLACK, SYSTEM }

@Serializable
enum class TextSize { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
