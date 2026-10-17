package com.zeroos.launcher.diagnostics

import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemorySamplerTest {
    private val logged = mutableListOf<Pair<LogLevel, String>>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            if (tag == MemorySampler.TAG) logged += level to message
        }
    }

    @Test
    fun nfrP4_samplesTenSecondsAfterZeroOsGoesToTheBackground() = runTest {
        val sampler = MemorySampler({ 48 }, logger, backgroundScope, 10.seconds)
        sampler.onScreenStopped()
        assertTrue(sampler.waiting)
        advanceTimeBy(9.seconds)
        assertTrue("not yet", logged.isEmpty())
        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(listOf(LogLevel.INFO to "memory in background: 48 MB"), logged)
        assertFalse(sampler.waiting)
    }

    @Test
    fun nfrP4_comingBackSoonerSkipsTheSample() = runTest {
        val sampler = MemorySampler({ 48 }, logger, backgroundScope, 10.seconds)
        sampler.onScreenStopped()
        advanceTimeBy(5.seconds)
        sampler.onScreenStarted()
        advanceTimeBy(20.seconds)
        runCurrent()
        assertTrue(logged.isEmpty())
    }

    @Test
    fun nfrP4_failedReadingIsLogged() = runTest {
        val sampler = MemorySampler({ throw IllegalStateException("no memory info") }, logger, backgroundScope)
        assertNull(sampler.sample(MemorySampler.ON_SCREEN))
        assertEquals(listOf(LogLevel.WARN to "memory check failed (on screen)"), logged)
    }

    @Test
    fun nfrP4_onScreenReadingIsLogged() = runTest {
        val sampler = MemorySampler({ 90 }, logger, backgroundScope)
        assertEquals(90, sampler.sample(MemorySampler.ON_SCREEN))
        assertEquals(listOf(LogLevel.INFO to "memory on screen: 90 MB"), logged)
    }
}
