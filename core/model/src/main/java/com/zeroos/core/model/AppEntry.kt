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

    companion object {
        /** Package name from a stored key string, e.g. "com.whatsapp" from "com.whatsapp/com.whatsapp.Main#0". */
        fun packageOf(key: String): String = key.substringBefore('/')
    }
}

/** Which profile an app belongs to (F2.10). Work and private-space apps are tagged in the app list, e.g. "Slack · work". */
enum class ProfileType { MAIN, CLONE, WORK, PRIVATE }

/**
 * An app shown by the launcher (F2.2). [copyNumber] is set when several apps share [label], e.g. WhatsApp and
 * its App Clone: 1 for the main profile's app, then 2, 3, … (shown as "I-WhatsApp", "II-WhatsApp", D-034).
 */
data class AppEntry(
    val key: AppKey,
    val label: String,
    val profile: ProfileType = ProfileType.MAIN,
    val copyNumber: Int? = null,
    /** False for system apps, so the menu doesn't offer "Uninstall" (F2.8). */
    val canUninstall: Boolean = true
) {
    val isMainProfile: Boolean get() = profile == ProfileType.MAIN
}
