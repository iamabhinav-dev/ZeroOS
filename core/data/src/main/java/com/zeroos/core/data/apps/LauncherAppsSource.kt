package com.zeroos.core.data.apps

import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType

/** A launchable activity as reported by the system. */
data class LaunchableActivity(
    val key: AppKey,
    val label: String,
    val profile: ProfileType = ProfileType.MAIN
)

/** System access needed by [AppsRepository]; [AndroidLauncherAppsSource] in the app, a fake in tests. */
interface LauncherAppsSource {
    /** All launchable activities in every profile the launcher can see. */
    fun launchableActivities(): List<LaunchableActivity>

    /** Calls [onChange] whenever apps are installed, removed, updated, or a profile becomes (un)available. */
    fun registerChangeListener(onChange: (packageName: String) -> Unit): ChangeListenerHandle

    /** Starts the app's main activity; throws if it can't be started. */
    fun launch(key: AppKey)

    fun interface ChangeListenerHandle {
        fun unregister()
    }
}
