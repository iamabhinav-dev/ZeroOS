package com.zeroos.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager

/**
 * Packages the screen-time lock never covers (F19.3, F19.8, D-062): ZeroOS itself, the default and the built-in
 * phone app (so calls and emergency calls always work) and Android's Settings app. Public APIs only.
 */
fun alwaysAllowedPackages(context: Context): Set<String> {
    val telecom = context.getSystemService(TelecomManager::class.java)
    val settings = context.packageManager
        .resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
    val systemDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) telecom?.systemDialerPackage else null
    return setOfNotNull(context.packageName, telecom?.defaultDialerPackage, systemDialer, settings)
}
