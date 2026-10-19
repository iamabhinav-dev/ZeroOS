package com.zeroos.core.data.insights

import com.zeroos.core.data.wellbeing.UsageEventSource
import com.zeroos.core.data.wellbeing.startOfDay
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads usage events for the last 7 days and builds the [UsageSummary] (F10.4: on the phone, nothing uploaded).
 * Called when home or Insights becomes visible, never on a timer (F10.9). Null without Usage access.
 */
class InsightsRepository(
    private val source: UsageEventSource,
    private val clock: Clock,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
    /** ZeroOS itself isn't counted as screen time. */
    private val ownPackage: String,
    private val unlocksSupported: Boolean,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) {
    suspend fun summary(): UsageSummary? = withContext(io) {
        val now = clock.instant()
        val zone = zone()
        val windowStart = startOfDay(now.atZone(zone).minusDays(DAYS - 1L).toInstant(), zone).toEpochMilli()
        try {
            summarize(source.events(windowStart, now.toEpochMilli()), now, zone, setOf(ownPackage), unlocksSupported)
        } catch (e: SecurityException) {
            logger.warn(TAG, "usage events not readable", e)
            null
        }
    }

    private companion object {
        const val TAG = "insights"
    }
}
