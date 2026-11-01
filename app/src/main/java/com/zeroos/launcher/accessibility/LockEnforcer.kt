package com.zeroos.launcher.accessibility

import com.zeroos.core.data.lock.LockState
import com.zeroos.core.data.lock.ScreenTimeLock
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The screen-time lock outside ZeroOS (F19.3, D-062), for the Accessibility service. When an app comes to the front
 * it reads the lock once: a launchable app the lock doesn't allow gets the lock screen; otherwise one timer checks
 * again when today's cap will be reached or the 15-minute unlock ends (NFR-P1, no polling). System windows,
 * keyboards and permission dialogs aren't launchable apps and are never covered.
 */
class LockEnforcer(
    private val lock: ScreenTimeLock,
    private val launchable: suspend () -> Set<String>,
    private val content: OverlayContentFactory,
    private val display: OverlayDisplay,
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val target: LockTarget
) {
    private var job: Job? = null

    /** A window of [packageName] came to the front ([className] is its activity or view). */
    fun onWindow(packageName: String, className: String?) {
        if (packageName == target.ownPackage) {
            // ZeroOS's own overlay windows aren't "an app in front"; its home is, and handles the lock itself.
            if (className == target.homeActivity) leave()
            return
        }
        job?.cancel()
        job = scope.launch { check(packageName) }
    }

    fun stop() {
        job?.cancel()
        display.hide()
    }

    private suspend fun check(packageName: String) {
        var wait: Long? = 0
        while (wait != null) {
            delay(wait)
            wait = checkOnce(packageName)
        }
    }

    /** Shows or takes away the lock screen for [packageName]; returns how long until the next check, or null. */
    private suspend fun checkOnce(packageName: String): Long? {
        val state = lock.refresh()
        return when {
            packageName !in launchable() -> null

            !state.allowsPackage(packageName) -> {
                show(packageName, state)
                null
            }

            else -> {
                if (display.showingFor != null) display.hide()
                nextCheckMillis(state)
            }
        }
    }

    /** When the lock could next come on while this app stays in front: the end of an unlock, or the cap. */
    private fun nextCheckMillis(state: LockState): Long? = when {
        !state.enabled -> null
        state.unlockedUntil > 0 -> state.unlockedUntil - target.clock.millis()
        else -> (state.capMinutes * MILLIS_PER_MINUTE - state.usedMillis).coerceAtLeast(MIN_RECHECK_MILLIS)
    }

    private suspend fun show(packageName: String, state: LockState) {
        if (display.showingFor == packageName) return
        logger.info(TAG, "lock screen over $packageName")
        display.show(
            content.build(Overlay.Lock(packageName, state.usedMillis)),
            OverlayActions(
                onGoHome = {
                    logger.info(TAG, "lock: went home from $packageName")
                    leave()
                    display.goHome()
                },
                onUnlock = {
                    logger.info(TAG, "lock: unlock asked for $packageName")
                    leave()
                    target.openUnlock(packageName)
                }
            )
        )
    }

    private fun leave() {
        job?.cancel()
        display.hide()
    }

    private companion object {
        const val TAG = "lock"
        const val MILLIS_PER_MINUTE = 60_000L

        /** Never re-check more often than this, even right at the cap. */
        const val MIN_RECHECK_MILLIS = 5_000L
    }
}

/** Where the lock screen sends people: ZeroOS's own package and home, and its unlock page for an app (F19.4). */
data class LockTarget(val ownPackage: String, val homeActivity: String, val clock: Clock, val openUnlock: (String) -> Unit)
