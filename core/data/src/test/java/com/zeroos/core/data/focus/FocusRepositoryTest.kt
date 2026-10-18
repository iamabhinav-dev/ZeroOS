package com.zeroos.core.data.focus

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FocusRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    /** The clock follows virtual time, so the session's own timer and "now" agree. */
    private fun TestScope.repository(): FocusRepository {
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")

            override fun withZone(zone: ZoneId) = this

            override fun instant(): Instant = Instant.parse("2026-10-18T09:00:00Z").plusMillis(testScheduler.currentTime)
        }
        val store = DataStoreFactory.create(FocusStateSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        return FocusRepository(store, clock, logger)
    }

    @Test
    fun f9_2_f9_4_sessionRunsForItsLengthThenEndsByItself() = runTest {
        val repo = repository()
        val seen = mutableListOf<FocusSession?>()
        backgroundScope.launch { repo.active.collect { seen += it } }
        runCurrent()
        repo.start(25, setOf("pkg/Main#0"))
        runCurrent()
        assertEquals(setOf("pkg/Main#0"), seen.last()?.allowed)
        advanceTimeBy(25 * 60_000L - 1)
        runCurrent()
        assertTrue("still running", seen.last() != null)
        advanceTimeBy(2)
        runCurrent()
        assertNull("time up", seen.last())
        assertTrue(logged.containsAll(listOf("focus started: 25 min, 1 apps allowed", "focus ended: time up")))
    }

    @Test
    fun f9_5_endingEarlyClearsIt() = runTest {
        val repo = repository()
        repo.start(50, emptySet())
        assertTrue(repo.active.first() != null)
        repo.end()
        assertNull(repo.active.first())
        assertTrue(logged.contains("focus ended early"))
    }

    @Test
    fun f9_3_savedSessionReadsBackExactly() = runTest {
        val state = FocusState(FocusSession(1_000, 2_000, setOf("a/B#0", "c/D#10")))
        val bytes = java.io.ByteArrayOutputStream().also { FocusStateSerializer.writeTo(state, it) }.toByteArray()
        assertEquals(state, FocusStateSerializer.readFrom(bytes.inputStream()))
    }
}
