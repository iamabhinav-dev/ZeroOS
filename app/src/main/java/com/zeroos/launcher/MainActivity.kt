package com.zeroos.launcher

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.launcher.diagnostics.DebugTestCrash
import com.zeroos.launcher.safemode.CrashLoopGuard
import com.zeroos.launcher.safemode.SafeModeApps
import com.zeroos.launcher.safemode.SafeModeRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var crashLoopGuard: CrashLoopGuard

    @Inject lateinit var diagnosticsSettings: DiagnosticsSettings

    @Inject lateinit var logStore: LogFileStore

    @Inject lateinit var logger: Logger

    /**
     * Back on home does nothing while ZeroOS is the default launcher (there's nowhere to go back to).
     * Opened as a normal app, it's disabled so Back leaves like in any app (Step 1.1).
     */
    private val ignoreBackOnHome = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = Unit
    }

    /** Emits when Home is pressed while ZeroOS is already in front (Step 1.1). */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Screens opened on top of home (app list, diagnostics) handle Back themselves.
        updateBackBehaviour()
        onBackPressedDispatcher.addCallback(this, ignoreBackOnHome)

        if (crashLoopGuard.isSafeMode) {
            showSafeMode()
            return
        }
        if (BuildConfig.DEBUG && diagnosticsSettings.crashOnStart) throw DebugTestCrash()
        setContent {
            ZeroTheme {
                LauncherRoot(homePresses, showDebugTools = BuildConfig.DEBUG)
            }
        }
    }

    /** F18.3–F18.5: only LauncherApps, core/logging and core/resources; no settings or feature modules. */
    private fun showSafeMode() {
        val apps = SafeModeApps(getSystemService(LauncherApps::class.java))
        setContent {
            ZeroTheme {
                SafeModeRoot(
                    apps = apps,
                    newestCrashReport = { logStore.crashReports().firstOrNull()?.readText().orEmpty() },
                    onHomeSettings = ::openHomeSettings,
                    onExit = {
                        logger.info(TAG, "safe mode exited")
                        crashLoopGuard.exitSafeMode()
                        recreate()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The default launcher can change while ZeroOS is in the background.
        updateBackBehaviour()
    }

    private fun updateBackBehaviour() {
        ignoreBackOnHome.isEnabled = isDefaultHome(packageManager, packageName)
    }

    private fun openHomeSettings() {
        val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
        val intent = if (homeSettings.resolveActivity(packageManager) != null) homeSettings else Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.tryEmit(Unit)
    }

    private companion object {
        const val TAG = "safemode"
    }
}
