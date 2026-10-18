package com.zeroos.feature.home

import com.zeroos.core.data.apps.renamedTo
import com.zeroos.core.data.focus.FocusSession
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.model.settings.FocusSettings
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.Wallpaper
import java.time.Instant
import java.time.ZonedDateTime

/** Everything the home screen shows (F1, wireframe A). */
data class HomeUiState(
    val time: ZonedDateTime,
    val favorites: List<AppEntry> = emptyList(),
    val alignment: Alignment = Alignment.LEFT,
    val batteryPercent: Int? = null,
    /** Shows the dot-matrix bolt next to the battery (D-031). */
    val charging: Boolean = false,
    val nextAlarm: ZonedDateTime? = null,
    val showClock: Boolean = true,
    /** hh:mm:ss (F6.7). */
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    /** For the favorite long-press menu (F2.8). */
    val renamedKeys: Set<String> = emptySet(),
    /** Side swipes open these apps; null = off (F4.2, F4.3, D-026). */
    val swipeLeftApp: AppEntry? = null,
    val swipeRightApp: AppEntry? = null,
    /** Double-tap on empty space locks the phone (F4.5). */
    val doubleTapToLock: Boolean = false,
    /** Phone wallpaper shows through, dimmed (F6.2). */
    val showWallpaper: Boolean = false,
    /** The focus slot (A4): shown when the button is on or a session runs (F9.1, F9.4). */
    val showFocus: Boolean = false,
    /** Minutes left in the running session, or null when idle (F9.4). */
    val focusMinutesLeft: Int? = null,
    /** How much of the session has passed, 0–1, for the filling dot line (F9.4). */
    val focusProgress: Float = 0f,
    val focusIcon: FocusIcon = FocusIcon.MIX
)

/**
 * Applies a running focus session (F9.3, F9.4): only allowed favorites, and the minutes left, rounded up so
 * "1m left" shows until the very end. With no session the slot shows the "focus" button if it's on (F9.1).
 */
fun HomeUiState.withFocus(settings: FocusSettings, session: FocusSession?): HomeUiState = copy(
    favorites = if (session == null) favorites else favorites.filter { session.allows(it.key.asString()) },
    showFocus = settings.showOnHome || session != null,
    focusMinutesLeft = session?.let {
        val left = it.endsAtMillis - time.toInstant().toEpochMilli()
        ((left + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
    },
    focusProgress = session?.let {
        val total = (it.endsAtMillis - it.startedAtMillis).coerceAtLeast(1)
        ((time.toInstant().toEpochMilli() - it.startedAtMillis).toFloat() / total).coerceIn(0f, 1f)
    } ?: 0f,
    focusIcon = settings.icon
)

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * Builds the home state. Favorites keep their saved order; uninstalled ones are skipped (F3.4); custom names
 * replace labels (F5.4). Battery and next alarm are shown only when their settings are on (F1.3, F1.8).
 */
fun homeUiState(
    time: ZonedDateTime,
    settings: LauncherSettings,
    apps: List<AppEntry>,
    battery: BatteryStatus?,
    nextAlarm: Instant?
): HomeUiState {
    val byKey = apps.associateBy { it.key.asString() }
    val favorites = settings.favorites
        .take(settings.home.maxFavorites)
        .mapNotNull { key -> byKey[key]?.renamedTo(settings.renamedApps[key]) }
    return HomeUiState(
        time = time,
        favorites = favorites,
        alignment = settings.home.alignment,
        batteryPercent = battery?.percent?.takeIf { settings.home.showBattery },
        charging = settings.home.showBattery && battery?.charging == true,
        nextAlarm = nextAlarm?.takeIf { settings.home.showNextAlarm }?.atZone(time.zone),
        showClock = settings.appearance.showClock,
        showSeconds = settings.appearance.showSeconds,
        showDate = settings.appearance.showDate,
        renamedKeys = settings.renamedApps.keys,
        swipeLeftApp = settings.gestures.swipeLeftApp?.let(byKey::get),
        swipeRightApp = settings.gestures.swipeRightApp?.let(byKey::get),
        doubleTapToLock = settings.gestures.doubleTapLock,
        showWallpaper = settings.appearance.wallpaper == Wallpaper.SYSTEM
    )
}
