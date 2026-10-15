package com.zeroos.core.data.apps

import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType

/** [LauncherAppsSource] backed by `LauncherApps` and `UserManager` (Step 1.2). Thin wrapper; logic lives in [AppsRepository]. */
class AndroidLauncherAppsSource(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val logger: Logger
) : LauncherAppsSource {

    private val loggedProfiles = mutableSetOf<Long>()

    override fun launchableActivities(): List<LaunchableActivity> = userManager.userProfiles.flatMap { user ->
        val serial = userManager.getSerialNumberForUser(user)
        val profile = profileOf(user, serial)
        launcherApps.getActivityList(null, user).map { info ->
            LaunchableActivity(
                key = AppKey(info.componentName.packageName, info.componentName.className, serial),
                label = info.label.toString(),
                profile = profile
            )
        }
    }

    /**
     * Profile type from Android's user type (Android 15+, available to the default launcher). Anything else
     * that isn't the main profile counts as a clone: the only extra profile on the test phone is App Clone (D-030).
     * Each profile's raw type is logged once (F17.7) to check this on the phone.
     */
    private fun profileOf(user: UserHandle, serial: Long): ProfileType {
        if (user == Process.myUserHandle()) return ProfileType.MAIN
        val userType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                launcherApps.getLauncherUserInfo(user)?.userType
            } catch (e: SecurityException) {
                logger.warn(TAG, "profile $serial type not readable", e)
                null
            }
        } else {
            null
        }
        val profile = profileFromUserType(userType)
        if (loggedProfiles.add(serial)) logger.info(TAG, "profile $serial: type=${userType ?: "unknown"} -> $profile")
        return profile
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

/** Maps Android's user type to a [ProfileType]; unknown or missing types count as clones (D-030). */
internal fun profileFromUserType(userType: String?): ProfileType = when (userType) {
    "android.os.usertype.profile.MANAGED" -> ProfileType.WORK
    "android.os.usertype.profile.PRIVATE" -> ProfileType.PRIVATE
    else -> ProfileType.CLONE
}

private const val TAG = "apps"
