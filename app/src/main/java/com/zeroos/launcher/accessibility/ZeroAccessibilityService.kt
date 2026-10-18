package com.zeroos.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.data.wellbeing.OutsideWatcher
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.resources.R
import com.zeroos.launcher.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ZeroOS's single Accessibility service (Step 4.2), shared by F4.4, F4.5, F8.7, F8.16 and later F16. Off by
 * default; the user turns it on after the explanation screen. It can't read screen content
 * (canRetrieveWindowContent is false): it only learns which app's window came to the front, and only for ZeroOS
 * and the apps wellbeing watches (D-023).
 */
@AndroidEntryPoint
class ZeroAccessibilityService : AccessibilityService() {
    @Inject lateinit var bridge: AccessibilityBridge

    @Inject lateinit var logger: Logger

    @Inject lateinit var watcher: OutsideWatcher

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var apps: AppsRepository

    @Inject lateinit var wellbeingState: WellbeingStateRepository

    private var scope: CoroutineScope? = null
    private var enforcer: OutsideEnforcer? = null

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> enforcer?.onScreenOff()
                Intent.ACTION_USER_PRESENT -> enforcer?.onScreenOn()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.connected(this)
        logger.info(TAG, "accessibility service connected")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope = it }
        val sizes = OverlayPunSizes(
            resources.getStringArray(R.array.wellbeing_puns_limit).size,
            resources.getStringArray(R.array.wellbeing_puns_break).size,
            resources.getStringArray(R.array.wellbeing_puns_break_button).size
        )
        enforcer = OutsideEnforcer(
            watcher,
            OverlayContentFactory(settings, apps, wellbeingState, sizes),
            OverlayWindow(this),
            scope,
            logger,
            ownPackage = packageName,
            homeActivity = MainActivity::class.java.name
        )
        scope.launch { watcher.watched.collect(::watchPackages) }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(Intent.ACTION_USER_PRESENT) }
        ContextCompat.registerReceiver(this, screen, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /** Window events only from ZeroOS and the watched apps (D-023); nothing else is ever seen. */
    private fun watchPackages(watched: Set<String>) {
        serviceInfo = serviceInfo.apply { packageNames = (watched + packageName).toTypedArray() }
        logger.info(TAG, "watching ${watched.size} apps outside ZeroOS")
        if (watched.isEmpty()) enforcer?.stop()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        enforcer?.onWindow(pkg, event.className?.toString())
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (scope != null) unregisterReceiver(screen)
        enforcer?.stop()
        scope?.cancel()
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
