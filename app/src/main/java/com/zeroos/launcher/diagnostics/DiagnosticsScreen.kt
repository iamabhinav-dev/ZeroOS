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

/**
 * Diagnostics with its sub-pages (F17.9): main page → crash reports → one report.
 * [openReport] jumps straight to a crash report, e.g. from the home crash banner (F17.10).
 */
@Composable
fun DiagnosticsRoute(
    onClose: () -> Unit,
    showDebugTools: Boolean,
    openReport: String? = null,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showReports by rememberSaveable { mutableStateOf(false) }
    var report by rememberSaveable { mutableStateOf(openReport) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportFailed = stringResource(R.string.diagnostics_export_failed)
    val chooserTitle = stringResource(R.string.diagnostics_export_chooser)

    when {
        report != null -> {
            var text by remember(report) { mutableStateOf("") }
            LaunchedEffect(report) { text = viewModel.readReport(report.orEmpty()) }
            CrashReportScreen(text = text, onBack = { if (openReport != null) onClose() else report = null })
        }

        showReports -> CrashReportsScreen(
            reports = state.crashReports,
            onOpen = { report = it.fileName },
            onBack = { showReports = false }
        )

        else -> DiagnosticsScreen(
            state = state,
            showDebugTools = showDebugTools,
            onFilter = viewModel::setFilter,
            onOpenReports = { showReports = true },
            onExport = {
                scope.launch {
                    val zip = viewModel.export()
                    if (zip == null) {
                        Toast.makeText(context, exportFailed, Toast.LENGTH_SHORT).show()
                    } else {
                        context.startActivity(shareIntent(context, zip, chooserTitle))
                    }
                }
            },
            onClear = viewModel::clear,
            onToggleLogging = { viewModel.setLoggingEnabled(!state.loggingEnabled) },
            onCycleLevel = viewModel::cycleLevel,
            onCrashNow = { throw DebugTestCrash() },
            onToggleCrashOnStart = { viewModel.setCrashOnStart(!state.crashOnStart) },
            onBack = onClose
        )
    }
}

/** Thrown by the debug-only "Crash now" button to test crash reports and safe mode (Phase 1 manual check). */
class DebugTestCrash : RuntimeException("Test crash from Diagnostics (debug only)")

private fun shareIntent(context: Context, zip: File, title: String): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.logs", zip)
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/zip")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** Wireframe C1. */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    showDebugTools: Boolean,
    onFilter: (LogFilter) -> Unit,
    onOpenReports: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    onToggleLogging: () -> Unit,
    onCycleLevel: () -> Unit,
    onCrashNow: () -> Unit,
    onToggleCrashOnStart: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = ZeroTheme.tokens
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .safeDrawingPadding()
            .testTag(DiagnosticsTestTags.ROOT)
    ) {
        item { Title(stringResource(R.string.diagnostics_title)) }
        item {
            Row(modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter - tokens.spacing.sectionGap)) {
                LogFilter.entries.forEach { filter ->
                    Action(
                        text = stringResource(filter.label()),
                        muted = filter != state.filter,
                        compact = true,
                        onClick = { onFilter(filter) }
                    )
                }
            }
        }
        if (state.visibleEntries.isEmpty()) {
            item { Body(stringResource(R.string.diagnostics_empty), muted = true) }
        }
        items(state.visibleEntries.size) { index ->
            val entry = state.visibleEntries[index]
            EntryRow(entry, expanded = expanded == index, onClick = { expanded = if (expanded == index) null else index })
        }
        item { Action(stringResource(R.string.diagnostics_crash_reports, state.crashReports.size), onClick = onOpenReports) }
        state.lastStop?.let { item { Body(stringResource(R.string.diagnostics_last_stop, it), muted = true) } }
        item {
            Row {
                Action(stringResource(R.string.diagnostics_export), onClick = onExport)
                Action(stringResource(R.string.diagnostics_clear), onClick = { confirmClear = true })
            }
        }
        item {
            val onOff = stringResource(if (state.loggingEnabled) R.string.common_on else R.string.common_off)
            SettingRow(stringResource(R.string.diagnostics_logging), onOff, onToggleLogging)
        }
        item { SettingRow(stringResource(R.string.diagnostics_level), stringResource(state.level.label()), onCycleLevel) }
        if (showDebugTools) {
            item { Action(stringResource(R.string.diagnostics_crash_now), onClick = onCrashNow) }
            item {
                val onOff = stringResource(if (state.crashOnStart) R.string.common_on else R.string.common_off)
                SettingRow(stringResource(R.string.diagnostics_crash_on_start), onOff, onToggleCrashOnStart)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.diagnostics_clear_confirm_title)) },
            text = { Text(stringResource(R.string.diagnostics_clear_confirm_message)) },
            confirmButton = {
                Action(stringResource(R.string.diagnostics_clear), onClick = {
                    confirmClear = false
                    onClear()
                })
            },
            dismissButton = { Action(stringResource(R.string.common_cancel), onClick = { confirmClear = false }) }
        )
    }
}

@Composable
private fun EntryRow(entry: StoredLogEntry, expanded: Boolean, onClick: () -> Unit) {
    val tokens = ZeroTheme.tokens
    val time = entry.time.substringAfter('T').substringBefore('.')
    val header = "$time  ${entry.level?.letter ?: '?'}  ${entry.tag}  ${entry.message}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.details.isNotEmpty(), onClick = onClick)
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
    ) {
        val color = if (entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR) tokens.colors.text else tokens.colors.textMuted
        Text(text = header, color = color, fontFamily = FontFamily.Monospace, fontSize = tokens.textSizes.mono)
        if (expanded) {
            entry.details.forEach {
                Text(text = it, color = tokens.colors.textMuted, fontFamily = FontFamily.Monospace, fontSize = tokens.textSizes.mono)
            }
        }
    }
}

private fun LogFilter.label(): Int = when (this) {
    LogFilter.ALL -> R.string.diagnostics_filter_all
    LogFilter.INFO -> R.string.diagnostics_filter_info
    LogFilter.WARN -> R.string.diagnostics_filter_warn
    LogFilter.ERROR -> R.string.diagnostics_filter_error
}

private fun LogLevel.label(): Int = when (this) {
    LogLevel.DEBUG -> R.string.diagnostics_level_debug
    LogLevel.INFO -> R.string.diagnostics_level_info
    LogLevel.WARN -> R.string.diagnostics_level_warn
    LogLevel.ERROR -> R.string.diagnostics_level_error
}

object DiagnosticsTestTags {
    const val ROOT = "diagnostics"
    const val CRASH_BANNER = "crash_banner"
}
