package com.zeroos.launcher.safemode

import java.io.File
import java.time.Clock
import java.time.Duration

/**
 * Detects crash loops (F18.1, F18.2, F18.6). Crashes within [window] of process start are recorded in
 * [file], written synchronously so the record survives the crash. When [threshold] are recorded, the next
 * start enters safe mode. Running for [window] without a crash, or exiting safe mode, clears the record.
 */
class CrashLoopGuard(
    private val file: File,
    private val clock: Clock,
    private val window: Duration = DEFAULT_WINDOW,
    private val threshold: Int = DEFAULT_THRESHOLD
) {
    private var startedAtMillis = clock.millis()

    /** Decided once at process start; stays the same for this process. */
    var isSafeMode: Boolean = false
        private set

    fun onProcessStart() {
        startedAtMillis = clock.millis()
        isSafeMode = recordedCrashes() >= threshold
    }

    /** Called from the crash handler. Only crashes soon after start count. */
    fun recordCrash() {
        if (clock.millis() - startedAtMillis > window.toMillis()) return
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText("${clock.millis()}\n")
        }
    }

    /** Called [window] after start if the app is still running: it's stable, so the count resets. */
    fun markStable() = clear()

    /** "Exit safe mode": reset and start normally next time (F18.6). */
    fun exitSafeMode() {
        clear()
        isSafeMode = false
    }

    fun recordedCrashes(): Int = runCatching { file.readLines().count { it.isNotBlank() } }.getOrDefault(0)

    private fun clear() {
        runCatching { file.delete() }
    }

    companion object {
        val DEFAULT_WINDOW: Duration = Duration.ofSeconds(60)
        const val DEFAULT_THRESHOLD = 3
    }
}
