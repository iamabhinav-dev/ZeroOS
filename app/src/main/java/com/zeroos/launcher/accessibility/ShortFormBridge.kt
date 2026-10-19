package com.zeroos.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.pm.PackageInfoCompat
import com.zeroos.core.data.shortform.ShortFormBlock
import com.zeroos.core.data.shortform.ShortFormDisplay
import com.zeroos.core.data.shortform.ShortFormGuard
import com.zeroos.core.data.shortform.ShortFormRules
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.model.settings.ShortFormSettings
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The short-video blocker inside the Accessibility service (F16.5, F16.6, F16.18). Window changes from any app only
 * tell it which app is in front; it reads screen structure only inside the enabled target apps, looking up the rule's
 * view IDs (no full-tree walk), at most once per [THROTTLE_MILLIS], off the main thread.
 */
class ShortFormBridge(
    private val service: AccessibilityService,
    private val rules: () -> ShortFormRules?,
    private val settings: StateFlow<ShortFormSettings?>,
    private val guard: ShortFormGuard,
    private val scope: CoroutineScope,
    private val worker: CoroutineDispatcher,
    private val clock: Clock
) {
    /** ZeroOS's home activity: a window of it means the user left the app. */
    private val homeActivity = com.zeroos.launcher.MainActivity::class.java.name

    private var lastCheck = 0L
    private var pending: Job? = null

    fun onEvent(type: Int, packageName: String, className: String?) {
        val s = settings.value?.takeIf { it.enabled } ?: return
        val r = rules() ?: return
        // ZeroOS's own overlay windows aren't "an app in front"; its home activity is.
        val ownOverlay = packageName == service.packageName && className != homeActivity
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !ownOverlay) guard.onForeground(packageName)
        if (packageName in s.blockedApps && r.app(packageName) != null) scheduleCheck(packageName, r)
    }

    fun stop() {
        pending?.cancel()
        guard.stop()
    }

    private fun scheduleCheck(packageName: String, r: ShortFormRules) {
        if (pending?.isActive == true) return
        val wait = (lastCheck + THROTTLE_MILLIS - clock.millis()).coerceAtLeast(0)
        pending = scope.launch {
            delay(wait)
            lastCheck = clock.millis()
            val found = withContext(worker) { isShortFormOnScreen(packageName, r) }
            guard.onDetection(packageName, found)
        }
    }

    /** Targeted lookups of the rule's view IDs in the front window, only for [packageName] (F16.6). */
    private fun isShortFormOnScreen(packageName: String, r: ShortFormRules): Boolean {
        val root = service.rootInActiveWindow?.takeIf { it.packageName?.toString() == packageName } ?: return false
        val present = r.app(packageName)?.fullViewIds.orEmpty()
            .filter { id -> root.findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser } }
            .toSet()
        val version = runCatching {
            PackageInfoCompat.getLongVersionCode(service.packageManager.getPackageInfo(packageName, 0))
        }.getOrNull()
        return r.isShortForm(packageName, present, version)
    }

    private companion object {
        /** F16.6, Appendix A: at most one check every 400 ms. */
        const val THROTTLE_MILLIS = 400L
    }
}

/** Shows the block screen (J3) in its own overlay window, with the pun or custom message and the pause for "Allow". */
class ShortFormOverlayDisplay(
    private val window: OverlayWindow,
    private val content: OverlayContentFactory,
    private val scope: CoroutineScope,
    private val guard: () -> ShortFormGuard
) : ShortFormDisplay {
    override fun show(block: ShortFormBlock) {
        scope.launch {
            val built = content.build(Overlay.ShortForm(block.packageName, block.allowLeft, block.autoHomeSeconds))
            window.show(built, OverlayActions(onGoHome = { guard().goHome() }, onAllow = { guard().allowFiveMinutes() }))
        }
    }

    override fun hide() = window.hide()

    override fun goHome() = window.goHome()
}
