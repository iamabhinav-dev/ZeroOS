package com.zeroos.core.data.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * True if pressing Home opens ZeroOS, i.e. it's the default launcher (Step 1.1). When no default is chosen,
 * Android resolves Home to its chooser, so this is false.
 */
fun isDefaultHome(packageManager: PackageManager, packageName: String): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
}

/** Whether ZeroOS is the default home app right now; a fake in tests (F14.7). */
fun interface DefaultHomeCheck {
    fun isDefaultHome(): Boolean
}

/**
 * Android's "default home app" dialog (`RoleManager.ROLE_HOME`, Android 10+), or the home settings screen where
 * the role isn't available (F14.2, F14.7).
 */
fun defaultHomeIntent(context: Context): Intent =
    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) homeRoleIntent(context) else null) ?: Intent(Settings.ACTION_HOME_SETTINGS)

@RequiresApi(Build.VERSION_CODES.Q)
private fun homeRoleIntent(context: Context): Intent? {
    val roles = context.getSystemService(RoleManager::class.java)
    return if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) roles.createRequestRoleIntent(RoleManager.ROLE_HOME) else null
}
