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

/** Which profile an app belongs to (F2.10). Apps outside [MAIN] are tagged in the app list, e.g. "WhatsApp · clone". */
enum class ProfileType { MAIN, CLONE, WORK, PRIVATE }

/** An app shown by the launcher (F2.2). */
data class AppEntry(
    val key: AppKey,
    val label: String,
    val profile: ProfileType = ProfileType.MAIN
) {
    val isMainProfile: Boolean get() = profile == ProfileType.MAIN
}
