package com.zeroos.core.data.wellbeing

import com.zeroos.core.model.settings.WellbeingSettings

/** Today's usage against the wellbeing rules, for the stop screens and the app list (F8.3–F8.5). */
class WellbeingStatus(private val usage: TodayUsage, private val state: WellbeingStateRepository) {

    /** The stop screen for [packageName] right now, or null to open it. */
    suspend fun stopFor(settings: WellbeingSettings, packageName: String): StopScreen? {
        val limited = settings.limitMinutesToday(packageName, 0) != null
        // Usage is read only when there's a limit to check (F8.8: no tracking otherwise).
        val used = if (limited) usage.foregroundToday()[packageName] ?: 0 else 0
        val extras = if (limited) state.extrasUsedToday()[packageName] ?: 0 else 0
        return settings.stopFor(packageName, used, extras)
    }

    /** Minutes left per limited app (F8.5); empty, without reading usage, when there are no active limits. */
    suspend fun minutesLeft(settings: WellbeingSettings): Map<String, Int> =
        if (!settings.hasActiveLimit) emptyMap() else settings.minutesLeft(usage.foregroundToday(), state.extrasUsedToday())
}
