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
import com.zeroos.core.data.shortform.ShortFormGuard
import com.zeroos.core.data.shortform.ShortFormRules
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.data.wellbeing.OutsideWatcher
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.settings.ShortFormSettings
import com.zeroos.core.resources.R
import com.zeroos.launcher.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    @Inject lateinit var gate: LaunchGate

    @Inject lateinit var clock: Clock

    private var scope: CoroutineScope? = null
    private var enforcer: OutsideEnforcer? = null
    private var shortForm: ShortFormBridge? = null
    private var watchedNow: Set<String> = emptySet()

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
            resources.getStringArray(R.array.wellbeing_puns_break_button).size,
            resources.getStringArray(R.array.shorts_puns).size,
            resources.getStringArray(R.array.wellbeing_puns_pause).size
        )
        val contentFactory = OverlayContentFactory(settings, apps, wellbeingState, sizes)
        enforcer = OutsideEnforcer(
            watcher,
            contentFactory,
            OverlayWindow(this),
            scope,
            logger,
            ownPackage = packageName,
            homeActivity = MainActivity::class.java.name
        )
        shortForm = startShortForm(scope, contentFactory)
        scope.launch {
            combine(watcher.watched, settings.settings.map { it.shortForm.enabled }) { w, on -> w to on }
                .distinctUntilChanged()
                .collect { (watched, on) -> configure(watched, on) }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(Intent.ACTION_USER_PRESENT) }
        ContextCompat.registerReceiver(this, screen, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /** The short-video blocker (F16): rules from the bundled asset, its own overlay window and guard. */
    private fun startShortForm(scope: CoroutineScope, contentFactory: OverlayContentFactory): ShortFormBridge {
        var rules: ShortFormRules? = null
        scope.launch {
            rules = withContext(Dispatchers.IO) { assets.open(RULES_FILE).bufferedReader().use { ShortFormRules.parse(it.readText()) } }
            logger.info(TAG, if (rules != null) "short videos: rules v${rules?.version} loaded" else "short videos: rules file not usable")
        }
        val shortFormSettings = settings.settings.map<_, ShortFormSettings?> { it.shortForm }.stateIn(scope, SharingStarted.Eagerly, null)
        lateinit var guard: ShortFormGuard
        val display = ShortFormOverlayDisplay(OverlayWindow(this), contentFactory, scope) { guard }
        guard = ShortFormGuard({ shortFormSettings.value }, { gate.focus.value != null }, wellbeingState, display, clock, scope, logger)
        @OptIn(ExperimentalCoroutinesApi::class)
        val worker = Dispatchers.Default.limitedParallelism(1)
        return ShortFormBridge(this, { rules }, shortFormSettings, guard, scope, worker, clock)
    }

    /**
     * Which apps' events the service receives (D-023, D-057): ZeroOS and the apps wellbeing watches; while the
     * short-video blocker is on, window changes from all apps (only the app name is used) plus content changes,
     * so the target apps' screens can be checked.
     */
    private fun configure(watched: Set<String>, shortFormOn: Boolean) {
        watchedNow = watched
        serviceInfo = serviceInfo.apply {
            packageNames = if (shortFormOn) null else (watched + packageName).toTypedArray()
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or (if (shortFormOn) AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED else 0)
        }
        logger.info(TAG, "watching ${watched.size} apps outside ZeroOS; short videos ${if (shortFormOn) "on" else "off"}")
        if (watched.isEmpty()) enforcer?.stop()
        if (!shortFormOn) shortForm?.stop()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val className = event.className?.toString()
        // Wellbeing outside ZeroOS only ever sees ZeroOS and its watched apps, as before (F8.7, F8.16).
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && (pkg == packageName || pkg in watchedNow)) {
            enforcer?.onWindow(pkg, className)
        }
        shortForm?.onEvent(event.eventType, pkg, className)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (scope != null) unregisterReceiver(screen)
        enforcer?.stop()
        shortForm?.stop()
        scope?.cancel()
        bridge.disconnected(this)
        logger.info(TAG, "accessibility service disconnected")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "accessibility"

        /** F16.12: the bundled detection rules. */
        const val RULES_FILE = "shortform_rules.json"
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
