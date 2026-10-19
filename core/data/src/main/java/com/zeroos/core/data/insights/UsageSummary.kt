package com.zeroos.core.data.insights

import com.zeroos.core.data.wellbeing.UsageEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One app's time today (F10.2). */
data class AppTime(val packageName: String, val millis: Long)

/** One day's screen time; [date] is the local date (F10.2). */
data class DayTotal(val date: LocalDate, val millis: Long)

/**
 * Screen time computed on the phone from usage events (F10.1–F10.5, F10.8, F10.12). [unlocks] is null where Android
 * doesn't report them (Android 8). [hourly] has 24 entries, today's local hours. [lastSevenDays] runs from six days
 * ago to today. [yesterdaySoFarMillis] is yesterday up to the same time of day, for a fair comparison.
 */
data class UsageSummary(
    val todayMillis: Long,
    val unlocks: Int?,
    val hourly: List<Long>,
    val topApps: List<AppTime>,
    val lastSevenDays: List<DayTotal>,
    val yesterdaySoFarMillis: Long
)

/** A stretch of time one app was in front. */
internal data class Interval(val packageName: String, val start: Long, val end: Long)

/**
 * When each app was in front between [windowStart] and [now]: an app already open at the start counts from the
 * start, one still open counts until now, repeated "resumed" events don't double up (same rules as F8.3).
 */
internal fun foregroundIntervals(events: List<UsageEvent>, windowStart: Long, now: Long): List<Interval> {
    val out = mutableListOf<Interval>()
    val openSince = mutableMapOf<String, Long>()
    val seen = mutableSetOf<String>()
    for (event in events.filter { it.type != UsageEvent.Type.UNLOCKED }.sortedBy { it.timeMillis }) {
        val time = event.timeMillis.coerceIn(windowStart, now)
        val pkg = event.packageName
        if (event.type == UsageEvent.Type.RESUMED) {
            openSince.putIfAbsent(pkg, time)
        } else {
            val from = openSince.remove(pkg) ?: windowStart.takeIf { pkg !in seen }
            if (from != null && time > from) out += Interval(pkg, from, time)
        }
        seen += pkg
    }
    openSince.forEach { (pkg, from) -> if (now > from) out += Interval(pkg, from, now) }
    return out
}

private fun List<Interval>.overlap(from: Long, to: Long): Long =
    sumOf { (minOf(it.end, to) - maxOf(it.start, from)).coerceAtLeast(0) }

private fun LocalDate.start(zone: ZoneId) = atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * Builds the [UsageSummary] for [now] in [zone]. Days and hours follow local time, so midnight, daylight saving and
 * time-zone changes land on the right day (F8.6 rules). Apps in [excluded] (ZeroOS itself) don't count as screen time.
 */
fun summarize(events: List<UsageEvent>, now: Instant, zone: ZoneId, excluded: Set<String>, unlocksSupported: Boolean): UsageSummary {
    val nowMillis = now.toEpochMilli()
    val today = now.atZone(zone).toLocalDate()
    val todayStart = today.start(zone)
    val firstDay = today.minusDays(DAYS - 1L)
    val all = foregroundIntervals(events, firstDay.start(zone), nowMillis).filter { it.packageName !in excluded }
    val days = (0 until DAYS).map { i ->
        val date = firstDay.plusDays(i.toLong())
        val end = if (date == today) nowMillis else date.plusDays(1).start(zone)
        DayTotal(date, all.overlap(date.start(zone), end))
    }
    val hourly = (0 until HOURS).map { h ->
        val from = today.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
        val to = if (h == HOURS - 1) today.plusDays(1).start(zone) else today.atTime(h + 1, 0).atZone(zone).toInstant().toEpochMilli()
        all.overlap(maxOf(from, todayStart), minOf(to, nowMillis))
    }
    val topApps = all.groupBy { it.packageName }
        .map { (pkg, list) -> AppTime(pkg, list.overlap(todayStart, nowMillis)) }
        .filter { it.millis > 0 }
        .sortedByDescending { it.millis }
        .take(TOP_APPS)
    val yesterdayStart = today.minusDays(1).start(zone)
    val sameTimeYesterday = now.atZone(zone).minusDays(1).toInstant().toEpochMilli()
    val unlocks = if (unlocksSupported) {
        events.count { it.type == UsageEvent.Type.UNLOCKED && it.timeMillis in todayStart..nowMillis }
    } else {
        null
    }
    return UsageSummary(
        todayMillis = days.last().millis,
        unlocks = unlocks,
        hourly = hourly,
        topApps = topApps,
        lastSevenDays = days,
        yesterdaySoFarMillis = all.overlap(yesterdayStart, sameTimeYesterday)
    )
}

/** F10.2: the last 7 days, today included. */
const val DAYS = 7
private const val HOURS = 24

/** F10.2: top 5 apps. */
const val TOP_APPS = 5
