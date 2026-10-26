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
import com.zeroos.core.data.docs.DocumentsGate
import com.zeroos.core.data.system.defaultHomeIntent
import com.zeroos.feature.applist.AppListRoute
import com.zeroos.feature.focus.FocusEndRoute
import com.zeroos.feature.focus.FocusRoute
import com.zeroos.feature.home.HomeRoute
import com.zeroos.feature.insights.HomeStatsRoute
import com.zeroos.feature.insights.InsightsRoute
import com.zeroos.feature.insights.UsageAccessRoute
import com.zeroos.feature.onboarding.OnboardingRoute
import com.zeroos.feature.onboarding.ReadBeforeUsingRoute
import com.zeroos.feature.onboarding.ReadBeforeUsingViewModel
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
    // Focus and Insights can be opened from home or a Settings page; Back returns there (D-051).
    var cameFrom by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    // The Usage access explanation returns to the screen that asked (F10.11).
    var accessReturn by rememberSaveable { mutableStateOf(LauncherScreen.HOME) }
    val open = { target: LauncherScreen, from: SettingsPage? ->
        cameFrom = from
        screen = target
    }
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

    if (!startPagesDone(onboardingCompleted)) return

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
                    onOpenFocus = { open(LauncherScreen.FOCUS, null) },
                    onEndFocusEarly = { screen = LauncherScreen.FOCUS_END },
                    stats = {
                        HomeStatsRoute(
                            onOpenInsights = { open(LauncherScreen.INSIGHTS, null) },
                            onAskAccess = {
                                accessReturn = LauncherScreen.HOME
                                screen = LauncherScreen.USAGE_ACCESS
                            }
                        )
                    }
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

            LauncherScreen.FOCUS, LauncherScreen.FOCUS_END, LauncherScreen.INSIGHTS, LauncherScreen.USAGE_ACCESS -> SideScreen(
                screen = screen,
                onBack = {
                    val (back, page) = backFrom(cameFrom)
                    settingsPage = page
                    screen = back
                },
                onHome = { screen = LauncherScreen.HOME },
                onAccessDone = { screen = accessReturn },
                onAskAccess = {
                    accessReturn = LauncherScreen.INSIGHTS
                    screen = LauncherScreen.USAGE_ACCESS
                }
            )

            LauncherScreen.SETTINGS -> SettingsRoute(
                onClose = { screen = LauncherScreen.HOME },
                onOpenDiagnostics = { screen = LauncherScreen.DIAGNOSTICS },
                onStartFocus = { open(LauncherScreen.FOCUS, SettingsPage.FOCUS) },
                onOpenInsights = { open(LauncherScreen.INSIGHTS, SettingsPage.SCREEN_TIME) },
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

/** Where Back from Focus or Insights goes: home, or the Settings page it was opened from (D-051). */
internal fun backFrom(page: SettingsPage?): Pair<LauncherScreen, SettingsPage> =
    if (page != null) LauncherScreen.SETTINGS to page else LauncherScreen.HOME to SettingsPage.MAIN

/** Screens opened from home or Settings: the Focus picker, ending focus early, Insights and Usage access. */
@Composable
private fun SideScreen(screen: LauncherScreen, onBack: () -> Unit, onHome: () -> Unit, onAccessDone: () -> Unit, onAskAccess: () -> Unit) {
    when (screen) {
        LauncherScreen.FOCUS -> FocusRoute(onBack = onBack, onStarted = onHome)
        LauncherScreen.FOCUS_END -> FocusEndRoute(onDone = onHome)
        LauncherScreen.INSIGHTS -> InsightsRoute(onAskAccess = onAskAccess, onBack = onBack)
        LauncherScreen.USAGE_ACCESS -> UsageAccessRoute(onDone = onAccessDone)
        else -> Unit
    }
}

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

/**
 * What comes before everything else: the release's documents (F14.8; first start and once after they change), then
 * onboarding (F14, once). Shows whichever is due and returns true only when both are done. The window stays black
 * while they load.
 */
@Composable
private fun startPagesDone(onboardingCompleted: Boolean?): Boolean {
    val documents: ReadBeforeUsingViewModel = hiltViewModel()
    val gate by documents.gate.collectAsStateWithLifecycle()
    return when {
        gate == DocumentsGate.LOADING || onboardingCompleted == null -> false

        gate != DocumentsGate.ACCEPTED -> {
            ReadBeforeUsingRoute(gate, documents)
            false
        }

        !onboardingCompleted -> {
            OnboardingRoute()
            false
        }

        else -> true
    }
}
