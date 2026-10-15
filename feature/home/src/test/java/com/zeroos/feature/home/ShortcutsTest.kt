package com.zeroos.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.AlarmClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Regression: tapping the clock crashed ZeroOS when the Clock app refused the request (SecurityException). */
@RunWith(AndroidJUnit4::class)
class ShortcutsTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    private fun contextThatThrows(error: RuntimeException) = object : ContextWrapper(app) {
        override fun startActivity(intent: Intent) = throw error
    }

    @Test
    fun f1_1_permissionDenialDoesNotCrash() {
        val error = startShortcut(contextThatThrows(SecurityException("requires SET_ALARM")), Intent(AlarmClock.ACTION_SHOW_ALARMS))
        assertTrue(error is SecurityException)
    }

    @Test
    fun f1_1_missingClockAppDoesNotCrash() {
        val error = startShortcut(contextThatThrows(ActivityNotFoundException()), Intent(AlarmClock.ACTION_SHOW_ALARMS))
        assertTrue(error is ActivityNotFoundException)
    }

    @Test
    fun f1_1_successReturnsNoError() {
        var started: Intent? = null
        val ok = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) {
                started = intent
            }
        }
        assertNull(startShortcut(ok, Intent(AlarmClock.ACTION_SHOW_ALARMS)))
        assertTrue(started!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
