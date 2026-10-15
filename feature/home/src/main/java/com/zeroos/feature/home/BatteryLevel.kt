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

/** Battery percentage for the home screen (F1.3), from the system battery broadcast (no polling, NFR-P5). */
class BatteryLevel(private val context: Context) {
    val percent: Flow<Int?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(percentOf(intent))
            }
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current value immediately.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sticky?.let { trySend(percentOf(it)) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    companion object {
        /** Percentage from a battery intent, or null if it has no level. */
        fun percentOf(intent: Intent): Int? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return if (level < 0 || scale <= 0) null else level * PERCENT / scale
        }

        private const val PERCENT = 100
    }
}
