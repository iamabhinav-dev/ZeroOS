package com.zeroos.core.data.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.SpecialPermission

/**
 * Checks and opens the special permissions (Step 4.1): "explain → open Android's screen → check again on return →
 * fallback". Every result is logged (F17.7).
 */
class SpecialPermissions(private val context: Context, private val logger: Logger) {

    fun isGranted(permission: SpecialPermission): Boolean = when (permission) {
        SpecialPermission.USAGE_ACCESS -> usageAccessGranted()
        SpecialPermission.ACCESSIBILITY -> accessibilityEnabled()
        SpecialPermission.DO_NOT_DISTURB -> context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    }

    /** Opens Android's screen for [permission]; falls back to the main Settings screen if the phone lacks it. */
    fun openSettings(permission: SpecialPermission) {
        logger.info(TAG, "opening settings for ${permission.logName}")
        open(settingsIntent(permission)) || open(Intent(Settings.ACTION_SETTINGS))
    }

    /** App info for ZeroOS, where "Allow restricted settings" lives on Android 13+ (sideloaded apps). */
    fun openAppInfo() {
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(PACKAGE_SCHEME, context.packageName, null)))
    }

    /** Called after returning from Android's screen, or when the user chose "Not now". */
    fun record(permission: SpecialPermission, granted: Boolean) =
        logger.info(TAG, "permission ${permission.logName} ${if (granted) "granted" else "not granted"}")

    internal fun settingsIntent(permission: SpecialPermission): Intent = when (permission) {
        SpecialPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        SpecialPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        SpecialPermission.DO_NOT_DISTURB -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    private fun open(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        logger.warn(TAG, "settings screen missing: ${intent.action}", e)
        false
    }

    private fun usageAccessGranted(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun accessibilityEnabled(): Boolean = context.getSystemService(AccessibilityManager::class.java)
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }

    private companion object {
        const val TAG = "permissions"
        const val PACKAGE_SCHEME = "package"
    }
}
