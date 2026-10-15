package com.zeroos.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Current time, re-emitted on every minute tick and when the time or time zone changes (F1.1, Step 1.3).
 * Uses the system's minute broadcast, which is aligned to the minute boundary, instead of a timer (NFR-P5).
 * The zone is read on every emission so a time-zone change shows immediately.
 */
class TimeTicks(
    private val context: Context,
    private val clock: Clock,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) {
    fun now(): ZonedDateTime = ZonedDateTime.ofInstant(clock.instant(), zone())

    val ticks: Flow<ZonedDateTime> = callbackFlow {
        trySend(now())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(now())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()
}
