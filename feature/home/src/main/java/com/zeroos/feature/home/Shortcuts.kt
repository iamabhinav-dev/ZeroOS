package com.zeroos.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Opens a clock/calendar shortcut (F1.1, F1.2). Returns the error instead of crashing when no app handles it,
 * or when the handling app refuses (e.g. a Clock app that requires the SET_ALARM permission).
 */
fun startShortcut(context: Context, intent: Intent): Exception? = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    null
} catch (e: ActivityNotFoundException) {
    e
} catch (e: SecurityException) {
    e
}
