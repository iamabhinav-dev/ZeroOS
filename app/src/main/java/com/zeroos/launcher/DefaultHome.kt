package com.zeroos.launcher

import android.content.Intent
import android.content.pm.PackageManager

/**
 * True if pressing Home opens ZeroOS, i.e. it's the default launcher (Step 1.1). When no default is chosen,
 * Android resolves Home to its chooser, so this is false.
 */
fun isDefaultHome(packageManager: PackageManager, packageName: String): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
}
