package com.zeroos.launcher.diagnostics

import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Memory samples for the log's Memory tab (NFR-P4). When ZeroOS's screen stops (another app opened), it waits
 * [idleDelay] so Android can release the screen's buffers, then logs "memory in background"; coming back sooner
 * skips that sample. A failed reading is logged as "memory check failed" instead of disappearing.
 */
class MemorySampler(
    private val reader: MemoryReader,
    private val logger: Logger,
    private val scope: CoroutineScope,
    private val idleDelay: Duration = DEFAULT_IDLE_DELAY
) {
    private var pending: Job? = null

    /** True while waiting to take the background sample. */
    val waiting: Boolean get() = pending?.isActive == true

    fun onScreenStopped() {
        pending?.cancel()
        pending = scope.launch {
            delay(idleDelay)
            sample(IN_BACKGROUND)
        }
    }

    fun onScreenStarted() {
        pending?.cancel()
        pending = null
    }

    /** Reads and logs ZeroOS's memory now; null if the reading failed. Call off the main thread. */
    fun sample(where: String): Int? = try {
        reader.megabytes().also { logger.info(TAG, "memory $where: $it MB") }
    } catch (e: IllegalStateException) {
        failed(where, e)
    } catch (e: SecurityException) {
        failed(where, e)
    }

    private fun failed(where: String, e: Exception): Int? {
        logger.warn(TAG, "memory check failed ($where)", e)
        return null
    }

    companion object {
        /** Log tag of every memory entry; the Diagnostics Memory tab shows only these. */
        const val TAG = "memory"
        const val IN_BACKGROUND = "in background"
        const val ON_SCREEN = "on screen"
        val DEFAULT_IDLE_DELAY = 10.seconds
    }
}
