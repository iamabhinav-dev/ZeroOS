package com.zeroos.launcher

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.data.system.defaultHomeIntent
import com.zeroos.feature.applist.AppListRoute
import com.zeroos.feature.focus.FocusEndRoute
import com.zeroos.feature.focus.FocusRoute
import com.zeroos.feature.home.HomeRoute
import com.zeroos.feature.onboarding.OnboardingRoute
import com.zeroos.feature.settings.SettingsPage
import com.zeroos.feature.settings.SettingsRoute
import com.zeroos.feature.wellbeing.StopHost
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
    // Back from the Focus picker returns to where it was opened: home, or Settings → Focus.
    var focusFromSettings by rememberSaveable { mutableStateOf(false) }
    var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    val crashReport by viewModel.crashReport.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val notHomeApp by viewModel.notHomeApp.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Checked each time ZeroOS comes to the front, and again when the default-home dialog closes (F14.7).
    LifecycleResumeEffect(Unit) {
        viewModel.refreshHomeApp()
        onPauseOrDispose {}
    }
    val homeDialog = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.refreshHomeApp() }

    LaunchedEffect(homePresses) {
        homePresses.collect {
            screen = LauncherScreen.HOME
            openReport = null
        }
    }
    LaunchedEffect(screen) { viewModel.onScreenShown(screen) }

    // Shown once, before anything else (F14); the window stays black while settings load.
    when (onboardingCompleted) {
        null -> return
        false -> return OnboardingRoute()
        true -> Unit
    }

    // A wellbeing stop screen covers everything while an app waits behind it (F8.1, F8.4).
    StopHost(homePresses) {
        when (screen) {
            LauncherScreen.HOME -> Box {
                HomeRoute(
                    onSwipeUp = { screen = LauncherScreen.APP_LIST },
                    // Swipe down opens search: the app list with its search field (F4.4).
                    // Swipe down: notification shade while Accessibility runs, otherwise search (F4.4, D-033).
                    onSwipeDown = { if (!viewModel.openNotificationShade()) screen = LauncherScreen.APP_LIST },
                    onLongPress = {
                        settingsPage = SettingsPage.MAIN
                        screen = LauncherScreen.SETTINGS
                    },
                    onDoubleTap = viewModel::lockScreen,
                    onOpenFocus = {
                        focusFromSettings = false
                        screen = LauncherScreen.FOCUS
                    },
                    onEndFocusEarly = { screen = LauncherScreen.FOCUS_END }
                )
                HomeBanners(
                    crashReport = crashReport,
                    notHomeApp = notHomeApp,
                    onViewReport = { report ->
                        viewModel.markCrashSeen()
                        openReport = report
                        screen = LauncherScreen.DIAGNOSTICS
                    },
                    onDismissCrash = viewModel::markCrashSeen,
                    onSetDefault = {
                        try {
                            homeDialog.launch(defaultHomeIntent(context))
                        } catch (e: ActivityNotFoundException) {
                            viewModel.onHomeDialogFailed(e)
                        }
                    },
                    onDismissHome = viewModel::dismissHomeApp,
                    modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding()
                )
            }

            LauncherScreen.APP_LIST -> AppListRoute(onClose = { screen = LauncherScreen.HOME })

            LauncherScreen.FOCUS -> FocusRoute(
                onBack = {
                    val (back, page) = focusBack(focusFromSettings)
                    settingsPage = page
                    screen = back
                },
                onStarted = { screen = LauncherScreen.HOME }
            )

            LauncherScreen.FOCUS_END -> FocusEndRoute(onDone = { screen = LauncherScreen.HOME })

            LauncherScreen.SETTINGS -> SettingsRoute(
                onClose = { screen = LauncherScreen.HOME },
                onOpenDiagnostics = { screen = LauncherScreen.DIAGNOSTICS },
                onStartFocus = {
                    focusFromSettings = true
                    screen = LauncherScreen.FOCUS
                },
                startPage = settingsPage
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
}

/** Where Back from the Focus picker goes: home, or Settings → Focus if it was opened there (D-051). */
internal fun focusBack(fromSettings: Boolean): Pair<LauncherScreen, SettingsPage> =
    if (fromSettings) LauncherScreen.SETTINGS to SettingsPage.FOCUS else LauncherScreen.HOME to SettingsPage.MAIN

/** The crash banner (F17.10) and "ZeroOS isn't your home app" (F14.7) at the top of home. */
@Composable
private fun HomeBanners(
    crashReport: String?,
    notHomeApp: Boolean,
    onViewReport: (String) -> Unit,
    onDismissCrash: () -> Unit,
    onSetDefault: () -> Unit,
    onDismissHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        crashReport?.let { report -> CrashBanner(onView = { onViewReport(report) }, onDismiss = onDismissCrash) }
        if (notHomeApp) NotHomeAppBanner(onSetDefault = onSetDefault, onDismiss = onDismissHome)
    }
}
