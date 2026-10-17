package com.zeroos.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ZeroOS's single Accessibility service (Step 4.2), shared by F4.4, F4.5 and later F8.7 and F16. Off by default;
 * the user turns it on after the explanation screen. It can't read screen content (canRetrieveWindowContent is
 * false) and only receives events from ZeroOS itself until a feature needs more (D-023).
 */
@AndroidEntryPoint
class ZeroAccessibilityService : AccessibilityService() {
    @Inject lateinit var bridge: AccessibilityBridge

    @Inject lateinit var logger: Logger

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.connected(this)
        logger.info(TAG, "accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        bridge.disconnected(this)
        logger.info(TAG, "accessibility service disconnected")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "accessibility"
    }
}

/** Connects the running service to the rest of ZeroOS as [SystemActions] (Step 4.2). */
@Singleton
class AccessibilityBridge @Inject constructor(private val logger: Logger) : SystemActions {
    private var service = WeakReference<AccessibilityService>(null)
    private val mutableRunning = MutableStateFlow(false)
    override val running: StateFlow<Boolean> = mutableRunning.asStateFlow()

    fun connected(service: AccessibilityService) {
        this.service = WeakReference(service)
        mutableRunning.value = true
    }

    fun disconnected(service: AccessibilityService) {
        if (this.service.get() === service) {
            this.service = WeakReference(null)
            mutableRunning.value = false
        }
    }

    override fun openNotificationShade(): Boolean = perform(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "notification shade")

    override fun lockScreen(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && perform(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "lock screen")

    private fun perform(action: Int, name: String): Boolean {
        val done = service.get()?.performGlobalAction(action) == true
        logger.info(TAG, "$name: ${if (done) "done" else "service not running"}")
        return done
    }

    private companion object {
        const val TAG = "accessibility"
    }
}
