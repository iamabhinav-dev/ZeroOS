package com.zeroos.feature.home

import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.LauncherSettings
import java.time.Instant
import java.time.ZonedDateTime

/** Everything the home screen shows (F1, wireframe A). */
data class HomeUiState(
    val time: ZonedDateTime,
    val favorites: List<AppEntry> = emptyList(),
    val alignment: Alignment = Alignment.LEFT,
    val batteryPercent: Int? = null,
    val nextAlarm: ZonedDateTime? = null,
    val showClock: Boolean = true,
    val showDate: Boolean = true
)

/**
 * Builds the home state. Favorites keep their saved order; uninstalled ones are skipped (F3.4); custom names
 * replace labels (F5.4). Battery and next alarm are shown only when their settings are on (F1.3, F1.8).
 */
fun homeUiState(
    time: ZonedDateTime,
    settings: LauncherSettings,
    apps: List<AppEntry>,
    batteryPercent: Int?,
    nextAlarm: Instant?
): HomeUiState {
    val byKey = apps.associateBy { it.key.asString() }
    val favorites = settings.favorites
        .take(settings.home.maxFavorites)
        .mapNotNull { key -> byKey[key]?.let { app -> app.copy(label = settings.renamedApps[key] ?: app.label) } }
    return HomeUiState(
        time = time,
        favorites = favorites,
        alignment = settings.home.alignment,
        batteryPercent = batteryPercent.takeIf { settings.home.showBattery },
        nextAlarm = nextAlarm?.takeIf { settings.home.showNextAlarm }?.atZone(time.zone),
        showClock = settings.appearance.showClock,
        showDate = settings.appearance.showDate
    )
}
