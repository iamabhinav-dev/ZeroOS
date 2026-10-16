package com.zeroos.launcher

import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.data.system.isDefaultHome
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.Wallpaper
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
            val launcher: LauncherViewModel = hiltViewModel()
            val appearance by launcher.appearance.collectAsStateWithLifecycle()
            ZeroTheme(appearance.theme, appearance.textSize) {
                val dark = ZeroTheme.tokens.isDark
                LaunchedEffect(appearance, dark) { applyWindowAppearance(appearance, dark) }
                LauncherRoot(homePresses, showDebugTools = BuildConfig.DEBUG, viewModel = launcher)
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

    /**
     * Window-level appearance (F6.2, F6.5): show the phone wallpaper behind home (screens draw their own solid
     * backgrounds, home draws a dim scrim), hide or show the status bar, and pick status bar icon colors.
     */
    private fun applyWindowAppearance(appearance: AppearanceSettings, dark: Boolean) {
        if (appearance.wallpaper == Wallpaper.SYSTEM) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable((if (dark) Color.BLACK else Color.WHITE).toDrawable())
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (appearance.showStatusBar) show(WindowInsetsCompat.Type.statusBars()) else hide(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
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
