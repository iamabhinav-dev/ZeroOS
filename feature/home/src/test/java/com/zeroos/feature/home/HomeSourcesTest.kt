package com.zeroos.feature.home

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class HomeSourcesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun batteryIntent(level: Int, scale: Int, status: Int = BatteryManager.BATTERY_STATUS_DISCHARGING, plugged: Int = 0) =
        Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_LEVEL, level)
            .putExtra(BatteryManager.EXTRA_SCALE, scale)
            .putExtra(BatteryManager.EXTRA_STATUS, status)
            .putExtra(BatteryManager.EXTRA_PLUGGED, plugged)

    @Test
    fun f1_3_chargingWhenChargingOrFullAndPlugged() {
        val charging = batteryIntent(50, 100, BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_PLUGGED_USB)
        val fullPlugged = batteryIntent(100, 100, BatteryManager.BATTERY_STATUS_FULL, BatteryManager.BATTERY_PLUGGED_AC)
        val fullUnplugged = batteryIntent(100, 100, BatteryManager.BATTERY_STATUS_FULL, 0)
        val discharging = batteryIntent(50, 100)
        assertEquals(BatteryStatus(50, true), BatteryLevel.statusOf(charging))
        assertEquals(true, BatteryLevel.isCharging(fullPlugged))
        assertEquals(false, BatteryLevel.isCharging(fullUnplugged))
        assertEquals(false, BatteryLevel.isCharging(discharging))
    }

    @Test
    fun f1_3_batteryPercentFromIntent() {
        assertEquals(82, BatteryLevel.percentOf(batteryIntent(82, 100)))
        assertEquals(50, BatteryLevel.percentOf(batteryIntent(1, 2)))
        assertNull(BatteryLevel.percentOf(Intent(Intent.ACTION_BATTERY_CHANGED)))
    }

    @Test
    fun f1_3_batteryFlowFollowsBroadcasts() = runTest {
        BatteryLevel(context).status.test {
            skipItems(if (context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)) != null) 1 else 0)
            context.sendStickyBroadcast(batteryIntent(41, 100))
            ShadowLooper.idleMainLooper()
            assertEquals(41, awaitItem()?.percent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun f1_8_nextAlarmFollowsAlarmChanges() = runTest {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val nextAlarm = NextAlarm(context, alarmManager)
        nextAlarm.time.test {
            assertNull(awaitItem())
            val trigger = Instant.parse("2026-10-17T01:00:00Z")
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger.toEpochMilli(), null),
                android.app.PendingIntent.getBroadcast(context, 0, Intent("x"), android.app.PendingIntent.FLAG_IMMUTABLE)
            )
            context.sendBroadcast(Intent(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
            ShadowLooper.idleMainLooper()
            assertEquals(trigger, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        shadowOf(alarmManager)
    }
}
