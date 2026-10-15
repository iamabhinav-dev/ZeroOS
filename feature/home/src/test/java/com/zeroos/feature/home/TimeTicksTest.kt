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
    private var zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val ticks = TimeTicks(context, clock) { zone }

    private fun broadcast(action: String) {
        context.sendBroadcast(Intent(action))
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun f1_1_emitsNowThenOnEveryMinuteTick() = runTest {
        ticks.ticks.test {
            assertEquals("15:59", awaitItem().toLocalTime().toString())
            clock.advanceBy(Duration.ofMinutes(1))
            broadcast(Intent.ACTION_TIME_TICK)
            assertEquals("16:00", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f1_1_updatesWhenTimeZoneChanges() = runTest {
        ticks.ticks.test {
            awaitItem()
            zone = ZoneId.of("Europe/London")
            broadcast(Intent.ACTION_TIMEZONE_CHANGED)
            assertEquals("11:29", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f1_1_updatesWhenClockIsSetManually() = runTest {
        ticks.ticks.test {
            awaitItem()
            clock.setTo(Instant.parse("2026-10-15T18:30:00Z"))
            broadcast(Intent.ACTION_TIME_CHANGED)
            assertEquals("00:00", awaitItem().toLocalTime().toString())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
