package com.zeroos.launcher

import android.content.Context
import com.zeroos.core.logging.FileLogger
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.LogcatMirror
import com.zeroos.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import javax.inject.Singleton

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
    fun fileLogger(store: LogFileStore, clock: Clock): FileLogger = FileLogger(
        store = store,
        clock = clock,
        // Toggle and level become user settings in Step 1.4 (F17.11); defaults until then.
        minLevel = { if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO },
        mirror = if (BuildConfig.DEBUG) LogcatMirror else { _ -> }
    )

    @Provides
    @Singleton
    fun logger(fileLogger: FileLogger): Logger = fileLogger
}
