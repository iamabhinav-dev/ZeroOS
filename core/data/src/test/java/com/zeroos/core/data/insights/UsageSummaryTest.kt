package com.zeroos.core.data.insights

import com.zeroos.core.data.wellbeing.UsageEvent
import com.zeroos.core.data.wellbeing.UsageEvent.Type.PAUSED
import com.zeroos.core.data.wellbeing.UsageEvent.Type.RESUMED
import com.zeroos.core.data.wellbeing.UsageEvent.Type.UNLOCKED
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.testing.MutableClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val MIN = 60_000L
private val ZONE: ZoneId = ZoneId.of("+01:00")

class UsageSummaryTest {
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()
    private fun e(pkg: String, type: UsageEvent.Type, iso: String) = UsageEvent(pkg, type, at(iso))

    @Test
    fun f10_1_f10_2_todayTopAppsAndZeroOsNotCounted() {
        val events = listOf(
            e("yt", RESUMED, "2026-10-18T08:00:00Z"),
            e("yt", PAUSED, "2026-10-18T08:52:00Z"),
            e("wa", RESUMED, "2026-10-18T09:00:00Z"),
            e("wa", PAUSED, "2026-10-18T09:31:00Z"),
            e("zeroos", RESUMED, "2026-10-18T09:31:00Z"),
            e("zeroos", PAUSED, "2026-10-18T09:40:00Z")
        )
        val s = summarize(events, Instant.parse("2026-10-18T10:00:00Z"), ZONE, setOf("zeroos"), unlocksSupported = true)
        assertEquals(83 * MIN, s.todayMillis)
        assertEquals(listOf(AppTime("yt", 52 * MIN), AppTime("wa", 31 * MIN)), s.topApps)
    }

    @Test
    fun f10_1_f10_12_unlocksTodayAndNoneOnAndroid8() {
        val events = listOf(
            e("android", UNLOCKED, "2026-10-17T20:00:00Z"), // yesterday (21:00 local)
            e("android", UNLOCKED, "2026-10-18T07:00:00Z"),
            e("android", UNLOCKED, "2026-10-18T09:00:00Z")
        )
        val now = Instant.parse("2026-10-18T10:00:00Z")
        assertEquals(2, summarize(events, now, ZONE, emptySet(), unlocksSupported = true).unlocks)
        assertNull(summarize(events, now, ZONE, emptySet(), unlocksSupported = false).unlocks)
    }

    @Test
    fun f10_2_f8_6_sessionAcrossMidnightIsSplitBetweenDays() {
        // 23:30 to 00:30 local: half yesterday, half today.
        val events = listOf(e("yt", RESUMED, "2026-10-17T22:30:00Z"), e("yt", PAUSED, "2026-10-17T23:30:00Z"))
        val s = summarize(events, Instant.parse("2026-10-18T09:00:00Z"), ZONE, emptySet(), true)
        assertEquals(30 * MIN, s.todayMillis)
        assertEquals(LocalDate.parse("2026-10-17"), s.lastSevenDays[5].date)
        assertEquals(30 * MIN, s.lastSevenDays[5].millis)
        assertEquals(7, s.lastSevenDays.size)
        assertEquals(LocalDate.parse("2026-10-18"), s.lastSevenDays.last().date)
    }

    @Test
    fun f10_5_hourlyBucketsInLocalHours() {
        // 08:40–10:10 local: 20 min in hour 8, 60 in hour 9, 10 in hour 10.
        val events = listOf(e("yt", RESUMED, "2026-10-18T07:40:00Z"), e("yt", PAUSED, "2026-10-18T09:10:00Z"))
        val s = summarize(events, Instant.parse("2026-10-18T11:00:00Z"), ZONE, emptySet(), true)
        assertEquals(24, s.hourly.size)
        assertEquals(listOf(20 * MIN, 60 * MIN, 10 * MIN), s.hourly.subList(8, 11))
        assertEquals(90 * MIN, s.hourly.sum())
    }

    @Test
    fun f10_5_hourlyOnTheDstDayAndAZoneChange() {
        val london = ZoneId.of("Europe/London")
        // 25 October 2026: clocks go back at 02:00 BST. Usage 00:30–03:30 local spans the repeated hour.
        val events = listOf(e("yt", RESUMED, "2026-10-24T23:30:00Z"), e("yt", PAUSED, "2026-10-25T03:30:00Z"))
        val s = summarize(events, Instant.parse("2026-10-25T12:00:00Z"), london, emptySet(), true)
        assertEquals("the day is 25 hours, nothing lost", 4 * 60 * MIN, s.todayMillis)
        assertEquals(s.todayMillis, s.hourly.sum())
        // Same usage seen from New York lands on the previous local day.
        val ny = summarize(events, Instant.parse("2026-10-25T12:00:00Z"), ZoneId.of("America/New_York"), emptySet(), true)
        assertEquals(4 * 60 * MIN, ny.lastSevenDays[5].millis)
    }

    @Test
    fun f10_8_yesterdayUpToTheSameTime() {
        val events = listOf(
            e("yt", RESUMED, "2026-10-17T08:00:00Z"),
            e("yt", PAUSED, "2026-10-17T09:00:00Z"), // yesterday 09–10 local
            e("yt", RESUMED, "2026-10-17T15:00:00Z"),
            e("yt", PAUSED, "2026-10-17T16:00:00Z") // yesterday afternoon
        )
        val s = summarize(events, Instant.parse("2026-10-18T10:00:00Z"), ZONE, emptySet(), true)
        assertEquals("only the morning counts against 11:00 today", 60 * MIN, s.yesterdaySoFarMillis)
        assertEquals(120 * MIN, s.lastSevenDays[5].millis)
    }

    @Test
    fun f8_6_clockSetBackNeverGivesNegativeTime() {
        val events = listOf(e("yt", RESUMED, "2026-10-18T12:00:00Z"), e("yt", PAUSED, "2026-10-18T13:00:00Z"))
        val s = summarize(events, Instant.parse("2026-10-18T10:00:00Z"), ZONE, emptySet(), true)
        assertEquals(0L, s.todayMillis)
        assertEquals(emptyList<AppTime>(), s.topApps)
    }

    @Test
    fun f10_4_repositoryReadsSevenDaysAndIsNullWithoutAccess() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-18T10:00:00Z"), ZONE)
        var asked: Pair<Long, Long>? = null
        val logger = object : Logger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
        }
        val repo = InsightsRepository(
            { from, to ->
                asked = from to to
                emptyList()
            },
            clock,
            UnconfinedTestDispatcher(testScheduler),
            logger,
            "zeroos",
            true,
            clock::getZone
        )
        repo.summary()
        assertEquals(at("2026-10-11T23:00:00Z") to clock.millis(), asked)
        val denied =
            InsightsRepository({ _, _ ->
                throw SecurityException()
            }, clock, UnconfinedTestDispatcher(testScheduler), logger, "zeroos", true)
        assertNull(denied.summary())
    }
}
