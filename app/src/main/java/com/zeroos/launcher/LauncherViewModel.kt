package com.zeroos.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.system.DefaultHomeCheck
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.logging.CrashNotice
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.settings.AppearanceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screens shown by [LauncherRoot]. */
enum class LauncherScreen(val logName: String) {
    HOME("home"),
    APP_LIST("app list"),
    SETTINGS("settings"),
    FOCUS("focus"),
    FOCUS_END("end focus"),
    INSIGHTS("insights"),
    USAGE_ACCESS("usage access"),
    DIAGNOSTICS("diagnostics")
}

/** App-level launcher state: the one-time crash banner (F17.10) and screen-change logging (F17.7). */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val crashNotice: CrashNotice,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher,
    settingsRepository: SettingsRepository,
    private val systemActions: SystemActions,
    private val defaultHome: DefaultHomeCheck
) : ViewModel() {

    /** Theme, text size, wallpaper and status bar for the whole app (F6). */
    val appearance: StateFlow<AppearanceSettings> = settingsRepository.settings.map { it.appearance }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    /** Whether onboarding is done (F14); null until settings have loaded, so it never flashes up by mistake. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.settings.map<_, Boolean?> { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var unseenReport: File? = null
    private val mutableCrashReport = MutableStateFlow<String?>(null)

    init {
        // Listing crash reports reads the disk: never on the main thread (NFR-P1, StrictMode).
        viewModelScope.launch {
            unseenReport = withContext(ioDispatcher) { crashNotice.unseenReport() }
            mutableCrashReport.value = unseenReport?.name
        }
    }

    /** File name of a crash report I haven't seen yet, or null once seen or dismissed. */
    val crashReport: StateFlow<String?> = mutableCrashReport.asStateFlow()

    /** Called for both "View report" and "Dismiss": either way the banner is shown only once. */
    fun markCrashSeen() {
        unseenReport?.let(crashNotice::markSeen)
        mutableCrashReport.value = null
    }

    private val mutableNotHomeApp = MutableStateFlow(false)

    /** True while home should show "ZeroOS isn't your home app" (F14.7). */
    val notHomeApp: StateFlow<Boolean> = mutableNotHomeApp.asStateFlow()

    /** ZeroOS came to the front (or the home dialog closed): check again; a dismissed line comes back (F14.7). */
    fun refreshHomeApp() {
        val isDefault = defaultHome.isDefaultHome()
        if (!isDefault) logger.info(TAG, "not the default home app")
        mutableNotHomeApp.value = !isDefault
    }

    fun dismissHomeApp() {
        logger.info(TAG, "not-home-app line dismissed")
        mutableNotHomeApp.value = false
    }

    /** The default-home dialog couldn't open (F14.7). */
    fun onHomeDialogFailed(e: Exception) = logger.warn(TAG, "could not open the default home dialog", e)

    /** Swipe down on home: the notification shade if Accessibility runs; false means "open search" (F4.4). */
    fun openNotificationShade(): Boolean = systemActions.openNotificationShade()

    /** Double-tap on home (F4.5); home only listens for it while the setting is on. */
    fun lockScreen() {
        systemActions.lockScreen()
    }

    fun onScreenShown(screen: LauncherScreen) = logger.info(TAG, "screen: ${screen.logName}")

    private companion object {
        const val TAG = "nav"
    }
}
