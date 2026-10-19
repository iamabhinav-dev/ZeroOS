package com.zeroos.core.data.shortform

import com.zeroos.core.data.settings.SettingsSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/** One supported app (F16.3): its short-form screen is recognised by any of [viewIds] (F16.12). */
@Serializable
data class ShortFormApp(
    @SerialName("package") val packageName: String,
    val name: String,
    /** Resource names without the package, e.g. "reel_recycler". */
    val viewIds: List<String>,
    /** Rules apply from this app version code; older versions aren't checked. */
    val minAppVersion: Long? = null
) {
    /** Full IDs as Android reports them: "com.google.android.youtube:id/reel_recycler". */
    val fullViewIds: List<String> get() = viewIds.map { "$packageName:id/$it" }
}

/** The bundled rules file, `assets/shortform_rules.json` (F16.12). */
@Serializable
data class ShortFormRules(val version: Int, val apps: List<ShortFormApp>) {
    fun app(packageName: String): ShortFormApp? = apps.firstOrNull { it.packageName == packageName }

    /** Short-form content is on screen when any of the app's view IDs is present (F16.6). */
    fun isShortForm(packageName: String, presentIds: Set<String>, appVersion: Long? = null): Boolean {
        val app = app(packageName) ?: return false
        val tooOld = app.minAppVersion != null && appVersion != null && appVersion < app.minAppVersion
        return !tooOld && app.fullViewIds.any { it in presentIds }
    }

    companion object {
        /** The newest rules format this build understands. */
        const val SUPPORTED_VERSION = 1

        /** Parses the rules; null (and the blocker stays inactive) if the file is broken or from a newer format. */
        fun parse(json: String): ShortFormRules? = try {
            SettingsSerializer.json.decodeFromString(serializer(), json).takeIf { it.version in 1..SUPPORTED_VERSION }
        } catch (@Suppress("SwallowedException") e: SerializationException) {
            null
        } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
            null
        }
    }
}
