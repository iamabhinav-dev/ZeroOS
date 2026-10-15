package com.zeroos.core.logging

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

/** Why the process last stopped (F17.6). */
enum class ExitReason(val description: String) {
    CRASH("crash"),
    NATIVE_CRASH("native crash"),
    ANR("not responding (ANR)"),
    LOW_MEMORY("low memory"),
    KILLED_BY_SYSTEM("killed by the system or phone maker"),
    USER("stopped by user"),
    SELF("exited normally"),
    UPDATED("app updated"),
    PERMISSION_CHANGED("permission changed"),
    OTHER("other")
}

data class ExitRecord(val timeMillis: Long, val reason: ExitReason, val detail: String?)

/** Source of past process exits; [AndroidExitInfoSource] on Android 11+, empty before. */
fun interface ExitInfoSource {
    fun recentExits(): List<ExitRecord>
}

@RequiresApi(Build.VERSION_CODES.R)
class AndroidExitInfoSource(private val activityManager: ActivityManager, private val packageName: String) : ExitInfoSource {
    override fun recentExits(): List<ExitRecord> =
        activityManager.getHistoricalProcessExitReasons(packageName, 0, MAX_RECORDS)
            .map { ExitRecord(it.timestamp, map(it.reason), it.description) }

    private fun map(reason: Int): ExitReason = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> ExitReason.CRASH

        ApplicationExitInfo.REASON_CRASH_NATIVE -> ExitReason.NATIVE_CRASH

        ApplicationExitInfo.REASON_ANR -> ExitReason.ANR

        ApplicationExitInfo.REASON_LOW_MEMORY -> ExitReason.LOW_MEMORY

        ApplicationExitInfo.REASON_SIGNALED,
        ApplicationExitInfo.REASON_OTHER,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE
        -> ExitReason.KILLED_BY_SYSTEM

        ApplicationExitInfo.REASON_USER_REQUESTED,
        ApplicationExitInfo.REASON_USER_STOPPED
        -> ExitReason.USER

        ApplicationExitInfo.REASON_EXIT_SELF -> ExitReason.SELF

        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> ExitReason.UPDATED

        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> ExitReason.PERMISSION_CHANGED

        else -> ExitReason.OTHER
    }

    private companion object {
        const val MAX_RECORDS = 10
    }
}

/**
 * Logs exits that happened since the last app start (F17.6), remembering the newest one seen in
 * [lastSeenFile] so each exit is logged once. Returns the newly logged exits, oldest first.
 */
class ExitReasonLogger(
    private val source: ExitInfoSource,
    private val lastSeenFile: File,
    private val logger: Logger
) {
    fun logNewExits(): List<ExitRecord> = runCatching {
        val lastSeen = lastSeenFile.takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull() ?: 0L
        val fresh = source.recentExits().filter { it.timeMillis > lastSeen }.sortedBy { it.timeMillis }
        fresh.forEach { exit ->
            val level = if (exit.reason == ExitReason.SELF || exit.reason == ExitReason.UPDATED) LogLevel.INFO else LogLevel.WARN
            logger.log(level, TAG, "previous stop: ${exit.reason.description}${exit.detail?.let { " ($it)" }.orEmpty()}")
        }
        fresh.lastOrNull()?.let {
            lastSeenFile.parentFile?.mkdirs()
            lastSeenFile.writeText(it.timeMillis.toString())
        }
        fresh
    }.getOrDefault(emptyList())

    companion object {
        const val TAG = "exit"
    }
}
