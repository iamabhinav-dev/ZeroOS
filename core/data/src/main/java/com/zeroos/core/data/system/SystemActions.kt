package com.zeroos.core.data.system

import kotlinx.coroutines.flow.StateFlow

/**
 * Phone-wide actions only ZeroOS's Accessibility service can perform (Step 4.2). Each returns false when the service
 * isn't running, so the caller can fall back (e.g. swipe down opens search instead of the shade, F4.4).
 */
interface SystemActions {
    /** True while ZeroOS's Accessibility service is connected. */
    val running: StateFlow<Boolean>

    /** Pulls down the notification shade (F4.4, D-033). */
    fun openNotificationShade(): Boolean

    /** Locks the phone like the power button (F4.5, Android 9+). */
    fun lockScreen(): Boolean
}
