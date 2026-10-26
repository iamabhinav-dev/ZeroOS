package com.zeroos.core.data.backup

import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.PhoneWallpaperHistory
import com.zeroos.core.model.settings.WellbeingSettings

/** Allowed values from REQUIREMENTS.md Appendix A. An imported file can't put a setting outside them (F13.5). */
internal object AllowedRanges {
    val maxFavorites = 1..10
    val pauseSeconds = 3..10
    val dailyLimitMinutes = 5..240
    val extraMinutesPerDay = 0..5
    val breakAfterMinutes = 5..60
    val breakWaitSeconds = 3..30
    val focusMinutes = 5..240
    val goalMinutes = 30..480
}

/**
 * These settings with every number pulled into its allowed range and lists cut to their limits, for a backup that
 * was edited by hand or damaged (D-059). Values already in range are left as they are.
 */
fun LauncherSettings.withinAllowedRanges(): LauncherSettings = copy(
    favorites = favorites.take(AllowedRanges.maxFavorites.last),
    home = home.copy(maxFavorites = home.maxFavorites.coerceIn(AllowedRanges.maxFavorites)),
    phoneWallpapers = PhoneWallpaperHistory(
        phoneWallpapers.home.take(PhoneWallpaperHistory.KEPT),
        phoneWallpapers.lock.take(PhoneWallpaperHistory.KEPT)
    ),
    wellbeing = wellbeing.withinAllowedRanges(),
    focus = focus.copy(lastMinutes = focus.lastMinutes.coerceIn(AllowedRanges.focusMinutes)),
    screenTime = screenTime.copy(goalMinutes = screenTime.goalMinutes?.coerceIn(AllowedRanges.goalMinutes))
)

private fun WellbeingSettings.withinAllowedRanges() = copy(
    pauseSeconds = pauseSeconds.coerceIn(AllowedRanges.pauseSeconds),
    extraMinutesPerDay = extraMinutesPerDay.coerceIn(AllowedRanges.extraMinutesPerDay),
    breakAfterMinutes = breakAfterMinutes.coerceIn(AllowedRanges.breakAfterMinutes),
    breakWaitSeconds = breakWaitSeconds.coerceIn(AllowedRanges.breakWaitSeconds),
    apps = apps.mapValues { (_, rule) -> rule.copy(dailyLimitMinutes = rule.dailyLimitMinutes?.coerceIn(AllowedRanges.dailyLimitMinutes)) }
)
