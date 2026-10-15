package com.zeroos.launcher.safemode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zeroos.launcher.diagnostics.CrashReportScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Safe mode with its one sub-page, the newest crash report (F18.3). */
@Composable
fun SafeModeRoot(
    apps: SafeModeApps,
    newestCrashReport: () -> String,
    onHomeSettings: () -> Unit,
    onExit: () -> Unit
) {
    var showReport by rememberSaveable { mutableStateOf(false) }
    val appList by produceState(emptyList<SafeModeApp>()) { value = withContext(Dispatchers.IO) { apps.load() } }

    if (showReport) {
        val report by produceState("") { value = withContext(Dispatchers.IO) { newestCrashReport() } }
        CrashReportScreen(text = report, onBack = { showReport = false })
    } else {
        SafeModeScreen(
            apps = appList,
            onAppClick = { apps.launch(it) },
            onViewReport = { showReport = true },
            onHomeSettings = onHomeSettings,
            onExit = onExit
        )
    }
}
