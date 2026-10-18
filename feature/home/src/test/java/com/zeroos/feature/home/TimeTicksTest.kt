package com.zeroos.feature.home

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.zeroos.core.testing.MutableClock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class TimeTicksTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = MutableClock(Instant.parse("2026-10-15T10:29:00Z"))
    private var zone: ZoneId = ZoneId.of("+01:00")
    private val ticks = TimeTicks(context, clock) { zone }

    private fun broadcast(action: String) {
        context.sendBroadcast(Intent(action))
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun f1_1_emitsNowThenOnEveryMinuteTick() = runTest {
        ticks.ticks.test {
            assertEquals("11:29", awaitItem().toLocalTime().toString())
            clock.advanceBy(Duration.ofMinutes(1))
            broadcast(Intent.ACTION_TIME_TICK)
            assertEquals("11:30", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f1_1_updatesWhenTimeZoneChanges() = runTest {
        ticks.ticks.test {
            awaitItem()
            zone = ZoneId.of("America/New_York")
            broadcast(Intent.ACTION_TIMEZONE_CHANGED)
            assertEquals("06:29", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f1_1_updatesWhenClockIsSetManually() = runTest {
        ticks.ticks.test {
            awaitItem()
            clock.setTo(Instant.parse("2026-10-15T23:00:00Z"))
            broadcast(Intent.ACTION_TIME_CHANGED)
            assertEquals("00:00", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f6_7_secondTicksLandOnEverySecond() = runTest {
        clock.setTo(Instant.parse("2026-10-15T10:29:07.250Z"))
        val seconds = TimeTicks(context, clock) { zone }
        seconds.secondTicks.test {
            assertEquals("11:29:07.250", awaitItem().toLocalTime().toString())
            // The next tick waits for the rest of this second (750 ms), then comes every 1 s.
            clock.advanceBy(Duration.ofMillis(750))
            assertEquals("11:29:08", awaitItem().toLocalTime().toString())
            clock.advanceBy(Duration.ofSeconds(1))
            assertEquals("11:29:09", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
