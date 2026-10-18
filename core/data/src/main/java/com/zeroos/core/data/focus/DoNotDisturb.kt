package com.zeroos.core.data.focus

import android.app.NotificationManager

/** The phone's Do Not Disturb mode (F9.7); a fake in tests. Changing it needs Do Not Disturb access. */
interface DoNotDisturb {
    fun hasAccess(): Boolean

    /** The current interruption filter (`NotificationManager.INTERRUPTION_FILTER_*`). */
    fun current(): Int

    fun set(filter: Int)

    companion object {
        /** Do Not Disturb as the user set it up (priority only), so their own exceptions such as alarms still apply. */
        const val ON = NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    /** No Do Not Disturb access: focus never touches it. */
    object Unavailable : DoNotDisturb {
        override fun hasAccess() = false

        override fun current() = NotificationManager.INTERRUPTION_FILTER_ALL

        override fun set(filter: Int) = Unit
    }
}

class AndroidDoNotDisturb(private val notifications: NotificationManager) : DoNotDisturb {
    override fun hasAccess() = notifications.isNotificationPolicyAccessGranted

    override fun current() = notifications.currentInterruptionFilter

    override fun set(filter: Int) = notifications.setInterruptionFilter(filter)
}
