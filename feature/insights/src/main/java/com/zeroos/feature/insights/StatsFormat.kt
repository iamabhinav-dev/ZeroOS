package com.zeroos.feature.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R
import kotlin.math.roundToInt

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60
private const val PERCENT = 100

/** "2h 14m", "4h" or "14m" (A3, H12). */
@Composable
internal fun duration(millis: Long): String {
    val minutes = (millis / MILLIS_PER_MINUTE).toInt()
    return when {
        minutes < MINUTES_PER_HOUR -> stringResource(R.string.duration_minutes, minutes)
        minutes % MINUTES_PER_HOUR == 0 -> stringResource(R.string.duration_hours, minutes / MINUTES_PER_HOUR)
        else -> stringResource(R.string.duration_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    }
}

/** "2h 14m · 38 unlocks", or just the time where unlocks aren't known (Android 8, F10.12). */
@Composable
internal fun timeAndUnlocks(time: String, unlocks: Int?): String =
    unlocks?.let { stringResource(R.string.stats_join, time, pluralStringResource(R.plurals.stats_unlocks, it, it)) } ?: time

/** Change against yesterday in whole percent, or null when yesterday had no screen time (F10.8). */
internal fun changePercent(today: Long, yesterday: Long): Int? =
    if (yesterday <= 0) null else ((today - yesterday) * PERCENT.toDouble() / yesterday).roundToInt()
