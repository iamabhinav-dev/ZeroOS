package com.zeroos.core.data.wellbeing

import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import java.time.Clock
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Reads what [outsideCheck] needs for the Accessibility service (F8.7, F8.16): settings, recent usage events and
 * today's "5 more minutes" uses. Remembers when each break ended, in memory only.
 */
class OutsideWatcher(
    private val settings: SettingsRepository,
    private val source: UsageEventSource,
    private val state: WellbeingStateRepository,
    private val clock: Clock,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) {
    private val lastBreak = ConcurrentHashMap<String, Long>()

    /** Apps the service should receive events from; empty when nothing outside ZeroOS is on (D-023). */
    val watched: Flow<Set<String>> = settings.settings.map { it.wellbeing.watchedPackages }.distinctUntilChanged()

    /** Checks [packageName] now; [knownInFront] when it has just come to the front. */
    suspend fun check(packageName: String, knownInFront: Boolean): OutsideCheck = withContext(io) {
        val wellbeing = settings.settings.first().wellbeing
        if (packageName !in wellbeing.watchedPackages) return@withContext OutsideCheck()
        val facts = facts(knownInFront) ?: return@withContext OutsideCheck()
        wellbeing.outsideCheck(
            packageName,
            facts.copy(extrasUsed = state.extrasUsedToday()[packageName] ?: 0, lastBreakMillis = lastBreak[packageName])
        )
    }

    /** The watched app in front right now, if any: used when the screen turns back on. */
    suspend fun appInFront(): String? = withContext(io) {
        val watched = settings.settings.first().wellbeing.watchedPackages
        if (watched.isEmpty()) return@withContext null
        val facts = facts(knownInFront = false) ?: return@withContext null
        watched.firstOrNull { currentSessionStart(facts.events, it, facts.windowStartMillis, facts.nowMillis) != null }
    }

    /** The break screen over [packageName] has gone; the next break is counted from now (F8.16). */
    fun breakTaken(packageName: String) {
        lastBreak[packageName] = clock.millis()
        logger.info(TAG, "break over for $packageName")
    }

    private fun facts(knownInFront: Boolean): OutsideFacts? {
        val now = clock.instant()
        val dayStart = startOfDay(now, zone()).toEpochMilli()
        val nowMillis = now.toEpochMilli()
        val windowStart = minOf(dayStart, nowMillis - LOOKBACK_MILLIS)
        val events = try {
            source.events(windowStart, nowMillis)
        } catch (e: SecurityException) {
            logger.warn(TAG, "usage events not readable", e)
            return null
        }
        return OutsideFacts(events, windowStart, dayStart, nowMillis, extrasUsed = 0, knownInFront = knownInFront)
    }

    private companion object {
        const val TAG = "wellbeing"

        /** Covers the longest break interval (60 min, F8.18) with room for short gaps. */
        const val LOOKBACK_MILLIS = 3 * 60 * 60_000L
    }
}
