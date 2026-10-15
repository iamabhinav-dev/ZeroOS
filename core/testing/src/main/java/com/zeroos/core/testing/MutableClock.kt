package com.zeroos.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Test clock that can be moved forward or to another time zone (Testing Policy #6). */
class MutableClock(
    private var now: Instant = Instant.parse("2026-10-15T05:00:00Z"),
    private var zoneId: ZoneId = ZoneId.of("Asia/Kolkata")
) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advanceBy(duration: Duration) {
        now = now.plus(duration)
    }

    fun setTo(instant: Instant) {
        now = instant
    }

    fun setZone(zone: ZoneId) {
        zoneId = zone
    }
}
