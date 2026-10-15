package com.zeroos.launcher.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.ExitReasonLogger
import com.zeroos.core.logging.LogExporter
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.StoredLogEntry
import com.zeroos.core.logging.info
import com.zeroos.core.logging.parseLogLines
import com.zeroos.core.logging.warn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Level filter chips (F17.9): each shows that level and above; ALL also shows DEBUG. */
enum class LogFilter(val minLevel: LogLevel) {
    ALL(LogLevel.DEBUG),
    INFO(LogLevel.INFO),
    WARN(LogLevel.WARN),
    ERROR(LogLevel.ERROR)
}

/** A crash report file and its exception line, e.g. "java.lang.IllegalStateException: boom". */
data class CrashReportItem(val fileName: String, val title: String)

data class DiagnosticsUiState(
    val entries: List<StoredLogEntry> = emptyList(),
    val filter: LogFilter = LogFilter.ALL,
    val crashReports: List<CrashReportItem> = emptyList(),
    /** Description of the last process stop, e.g. "killed by the system or phone maker". */
    val lastStop: String? = null,
    val loggingEnabled: Boolean = true,
    val level: LogLevel = LogLevel.INFO,
    /** Debug builds only (Step 1.5). */
    val crashOnStart: Boolean = false
) {
    val visibleEntries: List<StoredLogEntry>
        get() = entries.filter { (it.level ?: LogLevel.DEBUG) >= filter.minLevel }
}

/** Diagnostics screen (F17.9–F17.11, wireframes C1–C2). */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val store: LogFileStore,
    private val settings: DiagnosticsSettings,
    private val exporter: LogExporter,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val mutableState = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = mutableState.asStateFlow()

    init {
        logger.info(TAG, "screen opened")
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val entries = withContext(ioDispatcher) { parseLogLines(store.readAllLinesOldestFirst()).take(MAX_ENTRIES) }
            val reports = withContext(ioDispatcher) { store.crashReports().map { CrashReportItem(it.name, crashTitle(it)) } }
            mutableState.update { current ->
                current.copy(
                    entries = entries,
                    crashReports = reports,
                    lastStop = entries.firstOrNull { it.tag == ExitReasonLogger.TAG }?.message?.removePrefix(EXIT_PREFIX),
                    loggingEnabled = settings.isLoggingEnabled,
                    level = settings.minLevel,
                    crashOnStart = settings.crashOnStart
                )
            }
        }
    }

    fun setFilter(filter: LogFilter) = mutableState.update { it.copy(filter = filter) }

    fun setLoggingEnabled(enabled: Boolean) {
        logger.info(TAG, "setting changed: logging_enabled=$enabled")
        settings.isLoggingEnabled = enabled
        mutableState.update { it.copy(loggingEnabled = enabled) }
    }

    /** Steps through DEBUG → INFO → WARN → ERROR → DEBUG. */
    fun cycleLevel() {
        val next = LogLevel.entries[(settings.minLevel.ordinal + 1) % LogLevel.entries.size]
        settings.minLevel = next
        logger.info(TAG, "setting changed: logging_level=$next")
        mutableState.update { it.copy(level = next) }
    }

    /** Debug builds only: crash on every start to test safe mode (Step 1.5). */
    fun setCrashOnStart(enabled: Boolean) {
        logger.info(TAG, "setting changed: debug_crash_on_start=$enabled")
        settings.crashOnStart = enabled
        mutableState.update { it.copy(crashOnStart = enabled) }
    }

    fun clear() {
        viewModelScope.launch {
            withContext(ioDispatcher) { store.clear() }
            logger.info(TAG, "logs cleared")
            refresh()
        }
    }

    /** Zips logs and crash reports; returns null (and logs) if that fails. */
    suspend fun export(): File? = withContext(ioDispatcher) {
        try {
            exporter.export().also { logger.info(TAG, "logs exported") }
        } catch (e: IOException) {
            logger.warn(TAG, "export failed", e)
            null
        }
    }

    suspend fun readReport(fileName: String): String = withContext(ioDispatcher) {
        store.crashReports().firstOrNull { it.name == fileName }?.readText().orEmpty()
    }

    private fun crashTitle(report: File): String =
        report.useLines { lines -> lines.firstOrNull { it.contains("Exception") || it.contains("Error") } }.orEmpty()

    private companion object {
        const val TAG = "diagnostics"
        const val EXIT_PREFIX = "previous stop: "
        const val MAX_ENTRIES = 500
    }
}
