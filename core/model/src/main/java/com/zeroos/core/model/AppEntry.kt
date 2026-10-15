package com.zeroos.core.model

/**
 * Identifies one launchable app activity in one profile (main, work, or app clone).
 * [userSerial] is the profile's stable serial number, so keys survive restarts.
 */
data class AppKey(
    val packageName: String,
    val activityName: String,
    val userSerial: Long
) {
    /** Stable string form for storage, e.g. favorites and renames (Phase 2). */
    fun asString(): String = "$packageName/$activityName#$userSerial"
}

/** An app shown by the launcher (F2.2). [isMainProfile] is false for work-profile and app-clone apps (F2.10). */
data class AppEntry(
    val key: AppKey,
    val label: String,
    val isMainProfile: Boolean
)
