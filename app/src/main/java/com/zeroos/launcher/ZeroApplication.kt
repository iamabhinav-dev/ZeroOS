package com.zeroos.launcher

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.zeroos.core.logging.AndroidExitInfoSource
import com.zeroos.core.logging.CrashHandler
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.ExitReasonLogger
import com.zeroos.core.logging.FileLogger
import com.zeroos.core.logging.LogFileStore
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.launcher.safemode.CrashLoopGuard
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ZeroApplication : Application() {

    @Inject lateinit var logger: FileLogger

    @Inject lateinit var logStore: LogFileStore

    @Inject lateinit var clock: Clock

    @Inject lateinit var crashLoopGuard: CrashLoopGuard

    @Inject lateinit var diagnosticsSettings: DiagnosticsSettings

    override fun onCreate() {
        super.onCreate()
        crashLoopGuard.onProcessStart()
        CrashHandler.install(logger, logStore, clock, appInfo(), onCrash = crashLoopGuard::recordCrash)
        logger.info(TAG, "app start: ${appInfo()}")
        if (crashLoopGuard.isSafeMode) {
            logger.warn(TAG, "safe mode: crashed ${crashLoopGuard.recordedCrashes()} times soon after start")
            // Don't keep crashing on purpose once safe mode has caught it (Step 1.5 test switch).
            diagnosticsSettings.crashOnStart = false
        }
        // Still running after the window: stable, so reset the crash count (F18.6).
        // Clearing the crash record touches the disk, so it runs off the main thread.
        Executors.newSingleThreadScheduledExecutor().schedule(
            crashLoopGuard::markStable,
            CrashLoopGuard.DEFAULT_WINDOW.toMillis(),
            TimeUnit.MILLISECONDS
        )
        Executors.newSingleThreadExecutor().execute {
            logStore.deleteExpired()
            logPreviousExit()
        }
        if (BuildConfig.DEBUG) enableStrictMode()
    }

    private fun appInfo() = "ZeroOS ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE}), " +
        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}"

    private fun logPreviousExit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val source = AndroidExitInfoSource(getSystemService(ActivityManager::class.java), packageName)
        ExitReasonLogger(source, File(filesDir, "logs/last_exit.txt"), logger).logNewExits()
    }

    /** Debug builds only: report main-thread disk/network access and leaks into the F17 log (no Logcat on the phone). */
    private fun enableStrictMode() {
        val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll()
        val vmPolicy = StrictMode.VmPolicy.Builder().detectAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = Executors.newSingleThreadExecutor()
            threadPolicy.penaltyListener(executor) { if (isOwnCode(it)) logger.warn(STRICT_MODE_TAG, "thread policy violation", it) }
            vmPolicy.penaltyListener(executor) { if (isOwnCode(it)) logger.warn(STRICT_MODE_TAG, "VM policy violation", it) }
        } else {
            threadPolicy.penaltyLog()
            vmPolicy.penaltyLog()
        }
        StrictMode.setThreadPolicy(threadPolicy.build())
        StrictMode.setVmPolicy(vmPolicy.build())
    }

    private companion object {
        const val TAG = "app"
        const val STRICT_MODE_TAG = "strictmode"
    }
}

/**
 * True if a StrictMode violation involves ZeroOS code. Violations raised only inside the system (e.g. the phone's
 * theme loader on every activity start) aren't ours to fix and would flood the log.
 */
internal fun isOwnCode(violation: Throwable): Boolean = violation.stackTrace.any { it.className.startsWith("com.zeroos.") }
