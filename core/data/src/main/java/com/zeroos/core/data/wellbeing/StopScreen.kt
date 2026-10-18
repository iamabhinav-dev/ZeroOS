package com.zeroos.core.data.wellbeing

import com.zeroos.core.model.settings.WellbeingSettings

/** A screen shown instead of opening an app right away (F8.1, F8.4). */
sealed interface StopScreen {
    /** Mindful pause (wireframe H6). */
    data class Pause(val seconds: Int, val intentionPrompt: Boolean) : StopScreen

    /** Today's limit is used up (wireframe H7); [extrasLeft] "5 more minutes" uses remain today. */
    data class Limit(val usedMinutes: Int, val extrasLeft: Int) : StopScreen
}

/** What one "5 more minutes" adds to today's limit (F8.4). */
const val EXTRA_MINUTES = 5

private const val MILLIS_PER_MINUTE = 60_000L

/** Today's limit for [packageName] in minutes, with "5 more minutes" uses added; null when it has none (F8.3, F8.9). */
fun WellbeingSettings.limitMinutesToday(packageName: String, extrasUsed: Int): Int? =
    apps[packageName]?.dailyLimitMinutes?.takeIf { enabled && limitsEnabled }?.let { it + EXTRA_MINUTES * extrasUsed }

/** True when opening [packageName] may need a stop screen; decided from settings alone so other apps open at once. */
fun WellbeingSettings.mayStop(packageName: String): Boolean {
    val rule = apps[packageName] ?: return false
    return enabled && ((pauseEnabled && rule.pause) || (limitsEnabled && rule.dailyLimitMinutes != null))
}

/**
 * The stop screen for opening [packageName], or null to open it now. A used-up limit wins over the pause:
 * pausing before an app that can't open anyway would only add a step (F8.1, F8.4).
 */
fun WellbeingSettings.stopFor(packageName: String, usedMillis: Long, extrasUsed: Int): StopScreen? {
    if (!mayStop(packageName)) return null
    val limit = limitMinutesToday(packageName, extrasUsed)
    return when {
        limit != null && usedMillis >= limit * MILLIS_PER_MINUTE ->
            StopScreen.Limit((usedMillis / MILLIS_PER_MINUTE).toInt(), (extraMinutesPerDay - extrasUsed).coerceAtLeast(0))

        pauseEnabled && apps.getValue(packageName).pause -> StopScreen.Pause(pauseSeconds, intentionPrompt)

        else -> null
    }
}

/** Minutes left today for every limited app, 0 when used up (F8.5); empty when limits are off. */
fun WellbeingSettings.minutesLeft(usage: Map<String, Long>, extrasUsed: Map<String, Int>): Map<String, Int> =
    apps.keys.mapNotNull { pkg ->
        val limit = limitMinutesToday(pkg, extrasUsed[pkg] ?: 0) ?: return@mapNotNull null
        val left = limit * MILLIS_PER_MINUTE - (usage[pkg] ?: 0)
        // Rounded up: "1m left" until it's really used up.
        pkg to ((left + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
    }.toMap()
