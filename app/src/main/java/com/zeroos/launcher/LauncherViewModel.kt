package com.zeroos.launcher

import androidx.lifecycle.ViewModel
import com.zeroos.core.logging.CrashNotice
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Screens shown by [LauncherRoot]. */
enum class LauncherScreen(val logName: String) {
    HOME("home"),
    APP_LIST("app list"),
    DIAGNOSTICS("diagnostics")
}

/** App-level launcher state: the one-time crash banner (F17.10) and screen-change logging (F17.7). */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val crashNotice: CrashNotice,
    private val logger: Logger
) : ViewModel() {

    private val unseenReport: File? = crashNotice.unseenReport()
    private val mutableCrashReport = MutableStateFlow(unseenReport?.name)

    /** File name of a crash report I haven't seen yet, or null once seen or dismissed. */
    val crashReport: StateFlow<String?> = mutableCrashReport.asStateFlow()

    /** Called for both "View report" and "Dismiss": either way the banner is shown only once. */
    fun markCrashSeen() {
        unseenReport?.let(crashNotice::markSeen)
        mutableCrashReport.value = null
    }

    fun onScreenShown(screen: LauncherScreen) = logger.info(TAG, "screen: ${screen.logName}")

    private companion object {
        const val TAG = "nav"
    }
}
