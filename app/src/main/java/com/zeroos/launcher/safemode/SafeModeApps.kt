package com.zeroos.launcher.safemode

import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import java.text.Collator

/** An app in the safe-mode list: original label, no renames or hidden apps (F18.3). */
data class SafeModeApp(val label: String, val component: ComponentName, val user: UserHandle)

/**
 * Reads apps straight from LauncherApps, independent of core/data and settings, so a bug there can't
 * break safe mode (F18.4, F18.5). Main profile only, to keep it minimal.
 */
class SafeModeApps(private val launcherApps: LauncherApps) {
    fun load(): List<SafeModeApp> {
        val user = Process.myUserHandle()
        val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
        return launcherApps.getActivityList(null, user)
            .map { SafeModeApp(it.label.toString(), it.componentName, user) }
            .sortedWith(compareBy(collator) { app: SafeModeApp -> app.label })
    }

    fun launch(app: SafeModeApp) = runCatching { launcherApps.startMainActivity(app.component, app.user, null, null) }.isSuccess
}
