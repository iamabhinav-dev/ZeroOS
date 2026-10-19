package com.zeroos.launcher.accessibility

import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wellbeing.OutsideWatcher
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.data.wellbeing.PunList
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppearanceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Everything the overlay window draws (wireframes H7b, H14); [buttonPun] is only used on the break screen. */
data class OverlayContent(
    val overlay: Overlay,
    val app: AppEntry,
    val pun: Int,
    val buttonPun: Int,
    val appearance: AppearanceSettings,
    /** The short-video blocker's own message instead of a pun (F16.9). */
    val message: String? = null,
    /** "Allow 5 minutes" first shows the mindful pause (F16.11). */
    val pauseSeconds: Int = 0,
    val pausePun: Int = 0
)

data class OverlayActions(
    val onGoHome: () -> Unit,
    val onMoreMinutes: () -> Unit = {},
    val onBreakDone: () -> Unit = {},
    val onAllow: () -> Unit = {}
)

/** The window over other apps; a fake in tests. */
interface OverlayDisplay {
    /** The package the overlay is showing over, or null. */
    val showingFor: String?

    fun show(content: OverlayContent, actions: OverlayActions)

    fun hide()

    /** Leaves the app for the ZeroOS home screen (GLOBAL_ACTION_HOME); Android doesn't let apps close others. */
    fun goHome()
}

/** Sizes of the pun lists used over other apps (F8.13, F8.16). */
data class OverlayPunSizes(val limit: Int, val breakLine: Int, val breakButton: Int, val shorts: Int = 1, val pause: Int = 1)

/** Builds [OverlayContent]: the app's name as shown in ZeroOS, the puns and the appearance. */
class OverlayContentFactory(
    private val settings: SettingsRepository,
    private val apps: AppsRepository,
    private val state: WellbeingStateRepository,
    private val sizes: OverlayPunSizes
) {
    suspend fun build(overlay: Overlay): OverlayContent {
        val current = settings.settings.first()
        val pkg = overlay.packageName
        val app = visibleApps(apps.apps.first(), current).firstOrNull { it.key.packageName == pkg } ?: AppEntry(AppKey(pkg, "", 0), pkg)
        // Puns are always shown (D-045).
        val (pun, button) = when (overlay) {
            is Overlay.Limit -> state.nextPun(PunList.LIMIT, sizes.limit) to 0
            is Overlay.Break -> state.nextPun(PunList.BREAK, sizes.breakLine) to state.nextPun(PunList.BREAK_BUTTON, sizes.breakButton)
            is Overlay.ShortForm -> state.nextPun(PunList.SHORTS, sizes.shorts) to state.nextPun(PunList.PAUSE, sizes.pause)
        }
        if (overlay is Overlay.ShortForm) {
            val s = current.shortForm
            return OverlayContent(
                overlay,
                app,
                pun,
                0,
                current.appearance,
                message = s.customMessage.takeIf { s.useCustomMessage && it.isNotBlank() },
                pauseSeconds = current.wellbeing.pauseSeconds,
                pausePun = button
            )
        }
        return OverlayContent(overlay, app, pun, button, current.appearance)
    }

    suspend fun useExtra(packageName: String) = state.useExtra(packageName)
}

/**
 * Limits outside ZeroOS (F8.7) and take a break (F8.16) for the Accessibility service. Each time a watched app
 * comes to the front it checks once and sets a single timer for the next moment something is due; no polling
 * (NFR-P1). Every overlay shown and every choice is logged (F17.7).
 */
class OutsideEnforcer(
    private val watcher: OutsideWatcher,
    private val content: OverlayContentFactory,
    private val display: OverlayDisplay,
    private val scope: CoroutineScope,
    private val logger: Logger,
    /** ZeroOS's package and home activity: a window of it means the app was left for home. */
    private val ownPackage: String,
    private val homeActivity: String
) {
    private var job: Job? = null

    /** A window of [packageName] came to the front ([className] is its activity or view). */
    fun onWindow(packageName: String, className: String?) {
        when {
            packageName == ownPackage -> if (className == homeActivity) leftForHome()
            display.showingFor == packageName -> Unit
            else -> watch(packageName, knownInFront = true)
        }
    }

    /** Screen off: nothing can be due while it's off. */
    fun onScreenOff() {
        job?.cancel()
    }

    /** Unlocked: the app in front may not send a new event, so check which watched app is there. */
    fun onScreenOn() {
        job?.cancel()
        job = scope.launch { watcher.appInFront()?.let { checkUntilDue(it, knownInFront = false) } }
    }

    fun stop() {
        job?.cancel()
        display.hide()
    }

    private fun leftForHome() {
        job?.cancel()
        if (display.showingFor != null) {
            logger.info(TAG, "outside: overlay closed, back on home")
            display.hide()
        }
    }

    private fun watch(packageName: String, knownInFront: Boolean) {
        job?.cancel()
        job = scope.launch { checkUntilDue(packageName, knownInFront) }
    }

    private suspend fun checkUntilDue(packageName: String, knownInFront: Boolean) {
        var inFront = knownInFront
        while (true) {
            val check = watcher.check(packageName, inFront)
            check.overlay?.let {
                show(it)
                return
            }
            delay(check.checkAgainInMillis ?: return)
            inFront = false
        }
    }

    private suspend fun show(overlay: Overlay) {
        val pkg = overlay.packageName
        logger.info(
            TAG,
            when (overlay) {
                is Overlay.Limit -> "outside: limit screen over $pkg (${overlay.usedMinutes} min used, ${overlay.extrasLeft} extra left)"

                is Overlay.Break -> "break screen over $pkg"

                // Shown by the short-video blocker, never by this enforcer.
                is Overlay.ShortForm -> "short videos screen over $pkg"
            }
        )
        display.show(
            content.build(overlay),
            OverlayActions(
                onGoHome = {
                    logger.info(TAG, "outside: went home from $pkg")
                    display.hide()
                    display.goHome()
                },
                onMoreMinutes = {
                    scope.launch {
                        content.useExtra(pkg)
                        display.hide()
                        watch(pkg, knownInFront = true)
                    }
                },
                onBreakDone = {
                    watcher.breakTaken(pkg)
                    display.hide()
                    watch(pkg, knownInFront = true)
                }
            )
        )
    }

    private companion object {
        const val TAG = "wellbeing"
    }
}
