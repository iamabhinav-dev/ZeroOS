package com.zeroos.core.data.shortform

import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.settings.ShortFormMode
import com.zeroos.core.model.settings.ShortFormSettings
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the block screen needs (wireframe J3): [allowLeft] "Allow 5 minutes" uses, 0 hides it (and during Focus). */
data class ShortFormBlock(val packageName: String, val allowLeft: Int, val autoHomeSeconds: Int?)

/** The block screen over the target app; a fake in tests. */
interface ShortFormDisplay {
    fun show(block: ShortFormBlock)

    fun hide()

    /** GLOBAL_ACTION_HOME: leaves the app for ZeroOS home (F16.10). */
    fun goHome()
}

/**
 * Decides when to block short videos (F16.7, F16.8, F16.10, F16.11, F16.16, F16.18). It's told which app is in front
 * and whether that app shows short-form content (from the rules), and keeps today's short-video time and block count.
 * One timer at a time for limit mode and "Allow 5 minutes"; no polling (NFR-P1).
 */
class ShortFormGuard(
    private val settings: () -> ShortFormSettings?,
    private val focusActive: () -> Boolean,
    private val state: WellbeingStateRepository,
    private val display: ShortFormDisplay,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val logger: Logger
) {
    private var foreground: String? = null
    private var watching: String? = null
    private var since: Long? = null
    private var showing: String? = null
    private var allowedUntil = 0L
    private var quietUntil = 0L
    private var timer: Job? = null

    /** A window of [packageName] came to the front (F16.18): leaving the target app takes the block screen away. */
    fun onForeground(packageName: String) {
        if (packageName == foreground) return
        leaveShortForm()
        foreground = packageName
        if (showing != null && showing != packageName) hideBlock()
    }

    /** The front app's screen was checked against the rules: [shortForm] when its short-video feed is on screen. */
    fun onDetection(packageName: String, shortForm: Boolean) {
        val s = settings()
        val applies = s != null && s.enabled && packageName in s.blockedApps && packageName == foreground
        when {
            !applies -> leaveShortForm()

            shortForm && watching == null -> {
                watching = packageName
                since = clock.millis()
                decide()
            }

            !shortForm -> leaveShortForm()
        }
    }

    /** "Go home" (or automatically after N seconds): home, then a short quiet moment so it doesn't flicker (F16.10). */
    fun goHome() {
        val pkg = showing ?: return
        logger.info(TAG, "short videos: went home from $pkg")
        quietUntil = clock.millis() + QUIET_AFTER_HOME_MILLIS
        hideBlock()
        leaveShortForm()
        display.goHome()
    }

    /** "Allow 5 minutes", after the mindful pause (F16.11). */
    fun allowFiveMinutes() {
        val pkg = showing ?: return
        logger.info(TAG, "short videos: allowed 5 minutes in $pkg")
        allowedUntil = clock.millis() + ALLOW_MILLIS
        hideBlock()
        since = clock.millis()
        scope.launch { state.recordShortForm(allowUses = 1) }
        recheckAt(allowedUntil)
    }

    /** The blocker was turned off or the service stopped: take everything down. */
    fun stop() {
        leaveShortForm()
        hideBlock()
    }

    private fun decide() {
        val pkg = watching ?: return
        val s = settings() ?: return
        val now = clock.millis()
        timer?.cancel()
        when {
            now < quietUntil -> recheckAt(quietUntil)

            now < allowedUntil && !focusActive() -> recheckAt(allowedUntil)

            focusActive() || s.mode == ShortFormMode.BLOCK -> block(pkg, s)

            else -> scope.launch {
                val used = state.shortFormToday().millis + (clock.millis() - (since ?: clock.millis()))
                val allowance = s.allowanceMinutes * MILLIS_PER_MINUTE
                if (used >= allowance) block(pkg, s) else recheckAt(clock.millis() + allowance - used)
            }
        }
    }

    private fun block(pkg: String, s: ShortFormSettings) {
        if (showing == pkg) return
        // Time under the block screen doesn't count; it starts again after "Allow 5 minutes".
        leaveShortForm(keepWatching = true)
        showing = pkg
        scope.launch {
            val allowLeft = if (focusActive()) 0 else (s.allowUsesPerDay - state.shortFormToday().allowUses).coerceAtLeast(0)
            state.recordShortForm(blocks = 1)
            logger.info(TAG, "short videos: blocked $pkg (rules v${ShortFormRules.SUPPORTED_VERSION}, ${s.mode})")
            if (showing == pkg) display.show(ShortFormBlock(pkg, allowLeft, s.autoHomeSeconds))
        }
    }

    private fun recheckAt(atMillis: Long) {
        timer?.cancel()
        timer = scope.launch {
            delay((atMillis - clock.millis()).coerceAtLeast(0))
            decide()
        }
    }

    private fun hideBlock() {
        if (showing == null) return
        showing = null
        display.hide()
    }

    /**
     * Stops counting short-video time and saves what was counted: on leaving the feed or the app, or when blocked
     * ([keepWatching]: still in the feed, under the block screen).
     */
    private fun leaveShortForm(keepWatching: Boolean = false) {
        if (!keepWatching) {
            timer?.cancel()
            watching = null
        }
        val start = since ?: return
        since = null
        val spent = clock.millis() - start
        scope.launch { state.recordShortForm(millis = spent) }
    }

    private companion object {
        const val TAG = "shortform"
        const val MILLIS_PER_MINUTE = 60_000L

        /** F16.11: "Allow 5 minutes". */
        const val ALLOW_MILLIS = 5 * MILLIS_PER_MINUTE

        /** F16.10, Appendix A: pause after "Go home" before blocking again. */
        const val QUIET_AFTER_HOME_MILLIS = 1_000L
    }
}
