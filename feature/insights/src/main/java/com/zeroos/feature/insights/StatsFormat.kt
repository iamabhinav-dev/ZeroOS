package com.zeroos.feature.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R
import com.zeroos.core.ui.durationText
import kotlin.math.roundToInt

private const val PERCENT = 100

/** "2h 14m", "4h" or "14m" (A3, H12); shared with the screen-time lock in core/ui. */
@Composable
internal fun duration(millis: Long): String = durationText(millis)

/** "2h 14m · 38 unlocks", or just the time where unlocks aren't known (Android 8, F10.12). */
@Composable
internal fun timeAndUnlocks(time: String, unlocks: Int?): String =
    unlocks?.let { stringResource(R.string.stats_join, time, pluralStringResource(R.plurals.stats_unlocks, it, it)) } ?: time

/** Change against yesterday in whole percent, or null when yesterday had no screen time (F10.8). */
internal fun changePercent(today: Long, yesterday: Long): Int? =
    if (yesterday <= 0) null else ((today - yesterday) * PERCENT.toDouble() / yesterday).roundToInt()
