package com.zeroos.core.data.apps

import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.zeroos.core.model.AppKey

/** [LauncherAppsSource] backed by `LauncherApps` and `UserManager` (Step 1.2). Thin wrapper; logic lives in [AppsRepository]. */
class AndroidLauncherAppsSource(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager
) : LauncherAppsSource {

    override fun launchableActivities(): List<LaunchableActivity> = userManager.userProfiles.flatMap { user ->
        val serial = userManager.getSerialNumberForUser(user)
        val isMain = user == Process.myUserHandle()
        launcherApps.getActivityList(null, user).map { info ->
            LaunchableActivity(
                key = AppKey(info.componentName.packageName, info.componentName.className, serial),
                label = info.label.toString(),
                isMainProfile = isMain
            )
        }
    }

    override fun registerChangeListener(onChange: (String) -> Unit): LauncherAppsSource.ChangeListenerHandle {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChange(packageName)

            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange(packageName)

            override fun onPackageChanged(packageName: String, user: UserHandle) = onChange(packageName)

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(onChange)

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(onChange)
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return LauncherAppsSource.ChangeListenerHandle { launcherApps.unregisterCallback(callback) }
    }

    override fun launch(key: AppKey) {
        val user = userManager.getUserForSerialNumber(key.userSerial)
            ?: error("profile ${key.userSerial} no longer exists")
        launcherApps.startMainActivity(ComponentName(key.packageName, key.activityName), user, null, null)
    }
}
