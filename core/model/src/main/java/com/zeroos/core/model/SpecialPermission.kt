package com.zeroos.core.model

/**
 * Permissions granted in Android's own settings screens rather than a dialog (Step 4.1, F14.4, NFR-S4).
 * Each is asked for only when a feature that needs it is switched on.
 */
enum class SpecialPermission(val logName: String) {
    /** Screen time and daily limits (F8.3, F10). */
    USAGE_ACCESS("usage access"),

    /** Limits outside ZeroOS, double-tap lock, notification shade (F8.7, F4.5, F4.4). */
    ACCESSIBILITY("accessibility"),

    /** Focus turning Do Not Disturb on and off (F9.7). */
    DO_NOT_DISTURB("do not disturb")
}
