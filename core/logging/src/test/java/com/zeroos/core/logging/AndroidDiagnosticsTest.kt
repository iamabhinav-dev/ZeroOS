package com.zeroos.core.logging

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager.ApplicationExitInfoBuilder
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
class AndroidDiagnosticsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [30])
    fun f17_6_mapsAndroidExitReasons() {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        listOf(
            ApplicationExitInfo.REASON_CRASH to 1L,
            ApplicationExitInfo.REASON_SIGNALED to 2L,
            ApplicationExitInfo.REASON_ANR to 3L
        ).forEach { (reason, time) ->
            shadowOf(activityManager).addApplicationExitInfo(
                ApplicationExitInfoBuilder.newBuilder()
                    .setProcessName(context.packageName)
                    .setReason(reason)
                    .setTimestamp(time)
                    .build()
            )
        }

        val exits = AndroidExitInfoSource(activityManager, context.packageName).recentExits().sortedBy { it.timeMillis }

        assertEquals(listOf(ExitReason.CRASH, ExitReason.KILLED_BY_SYSTEM, ExitReason.ANR), exits.map { it.reason })
    }

    @Test
    fun f17_1_logcatMirrorUsesMatchingPriority() {
        LogcatMirror(LogEntry(0, LogLevel.WARN, "home", "slow"))
        val logged = ShadowLog.getLogsForTag("ZeroOS/home").single()
        assertEquals(android.util.Log.WARN, logged.type)
        assertEquals("slow", logged.msg)
    }
}
