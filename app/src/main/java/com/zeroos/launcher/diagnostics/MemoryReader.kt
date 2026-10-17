package com.zeroos.launcher.diagnostics

import android.os.Debug

/**
 * ZeroOS's own memory use in MB (NFR-P4, Step 3.2): the proportional set size Android reports for an app, read with
 * a public API, so no Developer options are needed. Takes a few milliseconds: call it off the main thread.
 */
fun interface MemoryReader {
    fun megabytes(): Int
}

/** [MemoryReader] for this process. */
class ProcessMemoryReader : MemoryReader {
    override fun megabytes(): Int = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss / KB_PER_MB

    private companion object {
        const val KB_PER_MB = 1024
    }
}
