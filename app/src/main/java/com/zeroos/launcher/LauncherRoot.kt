package com.zeroos.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.feature.applist.AppListRoute
import com.zeroos.feature.home.HomeRoute
import com.zeroos.feature.settings.SettingsRoute
import com.zeroos.launcher.diagnostics.CrashBanner
import com.zeroos.launcher.diagnostics.DiagnosticsRoute
import kotlinx.coroutines.flow.Flow

/**
 * Switches between home, the app list, Settings and Diagnostics (Steps 1.3, 1.4, 2.11). Pressing Home while ZeroOS is in
 * front returns to the home screen (Step 1.1). The crash banner shows on home once per new crash (F17.10).
 */
@Composable
fun LauncherRoot(homePresses: Flow<Unit>, showDebugTools: Boolean, viewModel: LauncherViewModel = hiltViewModel()) {
    var screen by rememberSaveable { mutableStateOf(LauncherScreen.HOME) }
    var openReport by rememberSaveable { mutableStateOf<String?>(null) }
    val crashReport by viewModel.crashReport.collectAsStateWithLifecycle()

    LaunchedEffect(homePresses) {
        homePresses.collect {
            screen = LauncherScreen.HOME
            openReport = null
        }
    }
    LaunchedEffect(screen) { viewModel.onScreenShown(screen) }

    when (screen) {
        LauncherScreen.HOME -> Box {
            HomeRoute(
                onSwipeUp = { screen = LauncherScreen.APP_LIST },
                // Swipe down opens search: the app list with its search field (F4.4).
                onSwipeDown = { screen = LauncherScreen.APP_LIST },
                onLongPress = { screen = LauncherScreen.SETTINGS }
            )
            crashReport?.let { report ->
                CrashBanner(
                    onView = {
                        viewModel.markCrashSeen()
                        openReport = report
                        screen = LauncherScreen.DIAGNOSTICS
                    },
                    onDismiss = viewModel::markCrashSeen,
                    modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding()
                )
            }
        }

        LauncherScreen.APP_LIST -> AppListRoute(onClose = { screen = LauncherScreen.HOME })

        LauncherScreen.SETTINGS -> SettingsRoute(
            onClose = { screen = LauncherScreen.HOME },
            onOpenDiagnostics = { screen = LauncherScreen.DIAGNOSTICS }
        )

        LauncherScreen.DIAGNOSTICS -> DiagnosticsRoute(
            onClose = {
                // Opened from a crash banner on home, or from Settings (wireframe F1).
                screen = if (openReport != null) LauncherScreen.HOME else LauncherScreen.SETTINGS
                openReport = null
            },
            showDebugTools = showDebugTools,
            openReport = openReport
        )
    }
}
