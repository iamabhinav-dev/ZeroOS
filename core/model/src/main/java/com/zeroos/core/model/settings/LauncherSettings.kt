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
    val wellbeing: WellbeingSettings = WellbeingSettings(),
    val focus: FocusSettings = FocusSettings(),
    val screenTime: ScreenTimeSettings = ScreenTimeSettings()
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
    /** Take a break (F8.16–F8.18, D-044); needs Accessibility and Usage access. */
    val breaksEnabled: Boolean = false,
    /** F8.18: 5–60 min straight in a chosen app before the break screen. */
    val breakAfterMinutes: Int = 15,
    /** F8.18: 3–30 s countdown after tapping the break button. */
    val breakWaitSeconds: Int = 5,
    /** Friction before turning Wellbeing or limits off (F8.12). */
    val disableFriction: DisableFriction = DisableFriction.HOLD,
    /** Rules by package name (F8.1, F8.3; D-041). */
    val apps: Map<String, AppRule> = emptyMap()
) {
    /** At least one daily limit is set and limits are on: turning off then needs the friction (F8.12). */
    val hasActiveLimit: Boolean get() = enabled && limitsEnabled && apps.values.any { it.dailyLimitMinutes != null }

    /** Apps the Accessibility service must watch: limits outside ZeroOS (F8.7) and take a break (F8.16). */
    val watchedPackages: Set<String>
        get() = if (!enabled) {
            emptySet()
        } else {
            apps.filterValues {
                (outsideEnforcement && limitsEnabled && it.dailyLimitMinutes != null) || (breaksEnabled && it.takeBreak)
            }.keys
        }
}

/** What Wellbeing does for one app (wireframe H5a). */
@Serializable
data class AppRule(val pause: Boolean = false, val dailyLimitMinutes: Int? = null, val takeBreak: Boolean = false) {
    val isEmpty: Boolean get() = !pause && dailyLimitMinutes == null && !takeBreak
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

/** Focus mode settings (F9); defaults from Appendix A. Works whether or not Wellbeing is on (F9.10). */
@Serializable
data class FocusSettings(
    /** The "focus" button on home (F9.1). */
    val showOnHome: Boolean = true,
    /** F9.8. */
    val icon: FocusIcon = FocusIcon.MIX,
    /** Turn on Do Not Disturb during focus (F9.7); needs DND access. */
    val doNotDisturb: Boolean = false,
    /** The picker's defaults next time: last duration and allowed apps (app keys, H11). */
    val lastMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val lastAllowed: List<String> = emptyList()
)

/** The four Focus icon designs (F9.8, docs/brand/focus-marks/). */
@Serializable
enum class FocusIcon { MIX, SPHERE, RIM, FLOW }

/** F9.2: the first preset. */
const val DEFAULT_FOCUS_MINUTES = 25

/** Screen-time stats on home and Insights (F10); defaults from Appendix A. */
@Serializable
data class ScreenTimeSettings(
    /** Stats on home (F10.3); on by default, shown once Usage access is granted. */
    val showOnHome: Boolean = true,
    /** F10.5. */
    val style: StatsStyle = StatsStyle.TEXT,
    /** Daily goal for the progress bar, or null when off (F10.7): 30 min–8 h. */
    val goalMinutes: Int? = null,
    /** "↓ 12% vs yesterday" (F10.8). */
    val compareYesterday: Boolean = false,
    /** "Tap to show screen time" was dismissed: stats stay hidden until turned on in Settings (F10.11). */
    val hintDismissed: Boolean = false
)

/** F10.5: how home shows screen time. */
@Serializable
enum class StatsStyle { TEXT, PROGRESS, WEEKLY, HOURLY }
