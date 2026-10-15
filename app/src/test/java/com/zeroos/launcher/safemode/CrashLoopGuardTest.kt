package com.zeroos.launcher.safemode

import com.zeroos.core.testing.MutableClock
import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashLoopGuardTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val clock = MutableClock()
    private val file by lazy { File(temp.root, "safe_mode/crashes.txt") }

    /** Simulates one process: starts, runs for [runFor], then crashes. */
    private fun crashAfter(runFor: Duration) {
        val guard = CrashLoopGuard(file, clock)
        guard.onProcessStart()
        clock.advanceBy(runFor)
        guard.recordCrash()
    }

    private fun nextStartIsSafe(): Boolean = CrashLoopGuard(file, clock).apply { onProcessStart() }.isSafeMode

    @Test
    fun f18_2_twoQuickCrashesStayNormal() {
        repeat(2) { crashAfter(Duration.ofSeconds(5)) }
        assertFalse(nextStartIsSafe())
    }

    @Test
    fun f18_2_threeQuickCrashesEnterSafeMode() {
        repeat(3) { crashAfter(Duration.ofSeconds(5)) }
        assertTrue(nextStartIsSafe())
    }

    @Test
    fun f18_2_crashesLongAfterStartDoNotCount() {
        repeat(3) { crashAfter(Duration.ofMinutes(5)) }
        assertFalse(nextStartIsSafe())
        assertEquals(0, CrashLoopGuard(file, clock).recordedCrashes())
    }

    @Test
    fun f18_6_stableRunResetsCount() {
        repeat(2) { crashAfter(Duration.ofSeconds(5)) }
        CrashLoopGuard(file, clock).apply { onProcessStart() }.markStable()
        crashAfter(Duration.ofSeconds(5))
        assertFalse(nextStartIsSafe())
    }

    @Test
    fun f18_6_exitSafeModeResetsAndStartsNormally() {
        repeat(3) { crashAfter(Duration.ofSeconds(5)) }
        val guard = CrashLoopGuard(file, clock).apply { onProcessStart() }
        assertTrue(guard.isSafeMode)
        guard.exitSafeMode()
        assertFalse(guard.isSafeMode)
        assertFalse(nextStartIsSafe())
    }

    @Test
    fun f18_1_recordingFailureNeverThrows() {
        val blocked = File(temp.root, "blocker").apply { writeText("file, not a folder") }
        val guard = CrashLoopGuard(File(blocked, "crashes.txt"), clock).apply { onProcessStart() }
        guard.recordCrash()
        assertEquals(0, guard.recordedCrashes())
    }
}
