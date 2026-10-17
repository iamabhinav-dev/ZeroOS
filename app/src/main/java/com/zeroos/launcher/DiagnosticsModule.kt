package com.zeroos.launcher

import android.content.Context
import com.zeroos.core.logging.CrashNotice
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.FileLogger
import com.zeroos.core.logging.LogExporter
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.LogcatMirror
import com.zeroos.core.logging.Logger
import com.zeroos.launcher.diagnostics.MemorySampler
import com.zeroos.launcher.diagnostics.ProcessMemoryReader
import com.zeroos.launcher.safemode.CrashLoopGuard
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Provides the app-wide clock and the F17 diagnostics logger. */
@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun logFileStore(@ApplicationContext context: Context, clock: Clock): LogFileStore =
        LogFileStore(File(context.filesDir, "logs"), clock)

    @Provides
    @Singleton
    fun diagnosticsSettings(@ApplicationContext context: Context): DiagnosticsSettings = DiagnosticsSettings(
        prefs = context.getSharedPreferences(DIAGNOSTICS_PREFS, Context.MODE_PRIVATE),
        defaultLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
    )

    @Provides
    @Singleton
    fun fileLogger(store: LogFileStore, clock: Clock, settings: DiagnosticsSettings): FileLogger = FileLogger(
        store = store,
        clock = clock,
        isEnabled = { settings.isLoggingEnabled },
        minLevel = { settings.minLevel },
        mirror = if (BuildConfig.DEBUG) LogcatMirror else { _ -> }
    )

    @Provides
    @Singleton
    fun memorySampler(logger: Logger): MemorySampler =
        MemorySampler(ProcessMemoryReader(), logger, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    @Provides
    fun logExporter(@ApplicationContext context: Context, store: LogFileStore, clock: Clock): LogExporter =
        LogExporter(store, File(context.cacheDir, "log_export"), clock)

    @Provides
    @Singleton
    fun crashLoopGuard(@ApplicationContext context: Context, clock: Clock): CrashLoopGuard =
        CrashLoopGuard(File(context.filesDir, "safe_mode/crashes.txt"), clock)

    @Provides
    fun crashNotice(store: LogFileStore, settings: DiagnosticsSettings): CrashNotice = CrashNotice(store, settings)

    @Provides
    @Singleton
    fun logger(fileLogger: FileLogger): Logger = fileLogger
}

/** SharedPreferences file of the diagnostics settings (D-025); included in Android Auto Backup (F13.3). */
internal const val DIAGNOSTICS_PREFS = "diagnostics"
