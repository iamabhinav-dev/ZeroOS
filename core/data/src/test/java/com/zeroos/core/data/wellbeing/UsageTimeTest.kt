package com.zeroos.core.data.wellbeing

import com.zeroos.core.data.wellbeing.UsageEvent.Type.PAUSED
import com.zeroos.core.data.wellbeing.UsageEvent.Type.RESUMED
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTimeTest {
    private val min = 60_000L

    private fun e(pkg: String, type: UsageEvent.Type, minute: Long) = UsageEvent(pkg, type, minute * min)

    @Test
    fun f8_3_sumsSessionsPerApp() {
        val events = listOf(
            e("yt", RESUMED, 10),
            e("yt", PAUSED, 25),
            e("ig", RESUMED, 30),
            e("ig", PAUSED, 35),
            e("yt", RESUMED, 40),
            e("yt", PAUSED, 50)
        )
        assertEquals(mapOf("yt" to 25 * min, "ig" to 5 * min), foregroundMillis(events, 0, 60 * min))
    }

    @Test
    fun f8_3_openAtStartCountsFromStartAndOpenNowCountsToNow() {
        val events = listOf(e("yt", PAUSED, 10), e("ig", RESUMED, 50))
        assertEquals(mapOf("yt" to 10 * min, "ig" to 10 * min), foregroundMillis(events, 0, 60 * min))
    }

    @Test
    fun f8_3_repeatedResumesDontDoubleCount() {
        val events = listOf(e("yt", RESUMED, 0), e("yt", RESUMED, 5), e("yt", PAUSED, 10), e("yt", PAUSED, 12))
        assertEquals(mapOf("yt" to 10 * min), foregroundMillis(events, 0, 60 * min))
    }

    @Test
    fun f8_6_dayStartsAtLocalMidnight() {
        val zone = ZoneId.of("Asia/Kolkata")
        assertEquals(Instant.parse("2026-10-16T18:30:00Z"), startOfDay(Instant.parse("2026-10-17T10:00:00Z"), zone))
        // Just after midnight it's a new day.
        assertEquals(Instant.parse("2026-10-17T18:30:00Z"), startOfDay(Instant.parse("2026-10-17T18:31:00Z"), zone))
    }

    @Test
    fun f8_6_dayStartAcrossDstAndTimeZoneChanges() {
        val london = ZoneId.of("Europe/London")
        // The day DST ends (25 hours long) still starts at local midnight.
        assertEquals(Instant.parse("2026-10-24T23:00:00Z"), startOfDay(Instant.parse("2026-10-25T12:00:00Z"), london))
        // Same moment, different zone: a different local day start.
        val now = Instant.parse("2026-10-17T02:00:00Z")
        assertEquals(Instant.parse("2026-10-16T18:30:00Z"), startOfDay(now, ZoneId.of("Asia/Kolkata")))
        assertEquals(Instant.parse("2026-10-16T23:00:00Z"), startOfDay(now, london))
    }

    @Test
    fun f8_6_clockSetBackwardsNeverGivesNegativeTime() {
        val events = listOf(e("yt", RESUMED, 50), e("yt", PAUSED, 70))
        // "Now" moved back to minute 40: events after now are clamped, nothing negative.
        assertEquals(0L, foregroundMillis(events, 0, 40 * min).getValue("yt"))
    }
}
