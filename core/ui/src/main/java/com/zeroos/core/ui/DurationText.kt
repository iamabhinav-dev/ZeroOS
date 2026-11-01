package com.zeroos.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60

/** "2h 14m", "4h" or "14m" (A3, H12, L1, L3). */
@Composable
fun durationText(millis: Long): String {
    val minutes = (millis / MILLIS_PER_MINUTE).toInt()
    return when {
        minutes < MINUTES_PER_HOUR -> stringResource(R.string.duration_minutes, minutes)
        minutes % MINUTES_PER_HOUR == 0 -> stringResource(R.string.duration_hours, minutes / MINUTES_PER_HOUR)
        else -> stringResource(R.string.duration_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    }
}
