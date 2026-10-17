package com.zeroos.core.data.wellbeing

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Build
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** One foreground change of an app, from Android's usage events (F8.3). */
data class UsageEvent(val packageName: String, val type: Type, val timeMillis: Long) {
    enum class Type { RESUMED, PAUSED }
}

/** Android's usage events; a fake in tests. Needs Usage access. */
fun interface UsageEventSource {
    fun events(startMillis: Long, endMillis: Long): List<UsageEvent>
}

/** [UsageEventSource] backed by `UsageStatsManager.queryEvents` (no polling: read only when needed, NFR-P5). */
class AndroidUsageEventSource(private val usageStats: UsageStatsManager) : UsageEventSource {
    override fun events(startMillis: Long, endMillis: Long): List<UsageEvent> {
        val events = usageStats.queryEvents(startMillis, endMillis)
        val out = mutableListOf<UsageEvent>()
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Type.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Type.PAUSED
                ACTIVITY_STOPPED -> UsageEvent.Type.PAUSED
                else -> null
            } ?: continue
            out += UsageEvent(event.packageName, type, event.timeStamp)
        }
        return out
    }

    private companion object {
        /** UsageEvents.Event.ACTIVITY_STOPPED (API 29); also ends a session on older Android. */
        val ACTIVITY_STOPPED = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) UsageEvents.Event.ACTIVITY_STOPPED else -1
    }
}

/** Local midnight that starts the day containing [now] (F8.6), correct across DST and time-zone changes. */
fun startOfDay(now: Instant, zone: ZoneId): Instant = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

/**
 * Foreground time per app between [startMillis] and [nowMillis] (F8.3). An app already open at the start counts
 * from the start; one still open counts until now. Repeated "resumed" events don't double-count.
 */
fun foregroundMillis(events: List<UsageEvent>, startMillis: Long, nowMillis: Long): Map<String, Long> {
    val totals = mutableMapOf<String, Long>()
    val openSince = mutableMapOf<String, Long>()
    val seen = mutableSetOf<String>()
    for (event in events.sortedBy { it.timeMillis }) {
        val time = event.timeMillis.coerceIn(startMillis, nowMillis)
        when (event.type) {
            UsageEvent.Type.RESUMED -> openSince.putIfAbsent(event.packageName, time)

            UsageEvent.Type.PAUSED -> {
                val from = openSince.remove(event.packageName) ?: startMillis.takeIf { event.packageName !in seen }
                if (from != null) totals.merge(event.packageName, time - from, Long::plus)
            }
        }
        seen += event.packageName
    }
    openSince.forEach { (pkg, from) -> totals.merge(pkg, nowMillis - from, Long::plus) }
    return totals
}

/** Today's foreground time per app, from local midnight until now (F8.3, F8.6). Read off the main thread. */
class TodayUsage(
    private val source: UsageEventSource,
    private val clock: Clock,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) {
    suspend fun foregroundToday(): Map<String, Long> = withContext(io) {
        val now = clock.instant()
        val start = startOfDay(now, zone()).toEpochMilli()
        try {
            foregroundMillis(source.events(start, now.toEpochMilli()), start, now.toEpochMilli())
        } catch (e: SecurityException) {
            // Usage access was taken away: no limits apply rather than a crash (fallback, AGENTS.md §5).
            logger.warn(TAG, "usage events not readable", e)
            emptyMap()
        }
    }

    private companion object {
        const val TAG = "wellbeing"
    }
}
