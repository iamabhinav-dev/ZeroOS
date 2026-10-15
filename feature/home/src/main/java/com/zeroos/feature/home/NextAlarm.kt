package com.zeroos.feature.home

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Time of the next alarm set in the Clock app, or null (F1.8). Updates when alarms change. */
class NextAlarm(private val context: Context, private val alarmManager: AlarmManager) {
    fun now(): Instant? = alarmManager.nextAlarmClock?.triggerTime?.let(Instant::ofEpochMilli)

    val time: Flow<Instant?> = callbackFlow {
        trySend(now())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(now())
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
