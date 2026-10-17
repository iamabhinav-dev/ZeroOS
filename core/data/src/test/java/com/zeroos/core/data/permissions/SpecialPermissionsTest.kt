package com.zeroos.core.data.permissions

import android.app.AppOpsManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.SpecialPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SpecialPermissionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val logged = mutableListOf<String>()
    private val permissions = SpecialPermissions(
        context,
        object : Logger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
                logged += message
            }
        }
    )

    @Test
    fun step4_1_usageAccessFollowsTheAppOp() {
        val appOps = shadowOf(context.getSystemService(AppOpsManager::class.java))
        appOps.setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_IGNORED)
        assertFalse(permissions.isGranted(SpecialPermission.USAGE_ACCESS))
        appOps.setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_ALLOWED)
        assertTrue(permissions.isGranted(SpecialPermission.USAGE_ACCESS))
    }

    @Test
    fun step4_1_doNotDisturbAccess() {
        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))
        notifications.setNotificationPolicyAccessGranted(false)
        assertFalse(permissions.isGranted(SpecialPermission.DO_NOT_DISTURB))
        notifications.setNotificationPolicyAccessGranted(true)
        assertTrue(permissions.isGranted(SpecialPermission.DO_NOT_DISTURB))
    }

    @Test
    fun step4_1_accessibilityOffUntilZeroOsServiceIsEnabled() {
        assertFalse(permissions.isGranted(SpecialPermission.ACCESSIBILITY))
    }

    @Test
    fun step4_1_opensAndroidsScreenForEachPermission() {
        val expected = mapOf(
            SpecialPermission.USAGE_ACCESS to Settings.ACTION_USAGE_ACCESS_SETTINGS,
            SpecialPermission.ACCESSIBILITY to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            SpecialPermission.DO_NOT_DISTURB to Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        )
        expected.forEach { (permission, action) ->
            permissions.openSettings(permission)
            assertEquals(action, shadowOf(context as Application).nextStartedActivity.action)
        }
        permissions.openAppInfo()
        val appInfo = shadowOf(context as Application).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appInfo.action)
        assertEquals("package:${context.packageName}", appInfo.dataString)
    }

    @Test
    fun f17_7_resultsAreLogged() {
        permissions.record(SpecialPermission.USAGE_ACCESS, granted = true)
        permissions.record(SpecialPermission.ACCESSIBILITY, granted = false)
        assertTrue("permission usage access granted" in logged)
        assertTrue("permission accessibility not granted" in logged)
    }
}
