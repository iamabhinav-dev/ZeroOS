package com.zeroos.feature.home

import com.zeroos.core.data.apps.renamedTo
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.Alignment
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
    /** Phone wallpaper shows through, dimmed (F6.2). */
    val showWallpaper: Boolean = false
)

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
        showWallpaper = settings.appearance.wallpaper == Wallpaper.SYSTEM
    )
}
