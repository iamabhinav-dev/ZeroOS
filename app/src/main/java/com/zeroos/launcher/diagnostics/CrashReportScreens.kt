package com.zeroos.launcher.diagnostics

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.StoredLogEntry
import com.zeroos.core.resources.R
import com.zeroos.core.ui.ZeroTheme
import java.io.File
import kotlinx.coroutines.launch

/** Wireframe C2. */
@Composable
fun CrashReportsScreen(reports: List<CrashReportItem>, onOpen: (CrashReportItem) -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    LazyColumn(modifier = Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding()) {
        item { Title(stringResource(R.string.crash_reports_title)) }
        if (reports.isEmpty()) item { Body(stringResource(R.string.crash_reports_empty), muted = true) }
        items(reports, key = { it.fileName }) { report ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.minTouchTarget)
                    .clickable { onOpen(report) }
                    .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
            ) {
                Text(text = report.fileName.removeSuffix(".txt"), color = tokens.colors.text, fontSize = tokens.textSizes.body)
                Text(
                    text = report.title,
                    color = tokens.colors.textMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = tokens.textSizes.mono
                )
            }
        }
    }
}

/** One full crash report: stack trace and the last 50 log entries. */
@Composable
fun CrashReportScreen(text: String, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(tokens.spacing.screenGutter)
    ) {
        Text(text = text, color = tokens.colors.text, fontFamily = FontFamily.Monospace, fontSize = tokens.textSizes.mono)
    }
}

/** Crash banner on home, shown once per new crash (F17.10, wireframe C3). */
@Composable
fun CrashBanner(onView: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    Column(modifier = modifier.fillMaxWidth().background(tokens.colors.background).testTag(DiagnosticsTestTags.CRASH_BANNER)) {
        Body(stringResource(R.string.crash_banner_message))
        Row {
            Action(stringResource(R.string.crash_banner_view), onClick = onView)
            Action(stringResource(R.string.common_dismiss), onClick = onDismiss)
        }
    }
}
