package com.zeroos.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Battery level and whether it's charging (F1.3). */
data class BatteryStatus(val percent: Int, val charging: Boolean)

/** Battery status for the home screen (F1.3), from the system battery broadcast (no polling, NFR-P5). */
class BatteryLevel(private val context: Context) {
    val status: Flow<BatteryStatus?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(statusOf(intent))
            }
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current value immediately.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sticky?.let { trySend(statusOf(it)) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    companion object {
        /** Status from a battery intent, or null if it has no level. */
        fun statusOf(intent: Intent): BatteryStatus? = percentOf(intent)?.let { BatteryStatus(it, isCharging(intent)) }

        /** Charging, or full while still plugged in (shows the bolt, D-031). */
        fun isCharging(intent: Intent): Boolean {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            return status == BatteryManager.BATTERY_STATUS_CHARGING || (status == BatteryManager.BATTERY_STATUS_FULL && plugged)
        }

        /** Percentage from a battery intent, or null if it has no level. */
        fun percentOf(intent: Intent): Int? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return if (level < 0 || scale <= 0) null else level * PERCENT / scale
        }

        private const val PERCENT = 100
    }
}
