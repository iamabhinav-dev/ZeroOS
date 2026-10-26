package com.zeroos.core.data.shortform

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.ShortFormMode
import com.zeroos.core.model.settings.ShortFormSettings
import com.zeroos.core.model.settings.YOUTUBE_PACKAGE
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private const val MIN = 60_000L
private const val YT = YOUTUBE_PACKAGE

@OptIn(ExperimentalCoroutinesApi::class)
class ShortFormGuardTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    private class FakeDisplay : ShortFormDisplay {
        var block: ShortFormBlock? = null
        var home = 0
        override fun show(block: ShortFormBlock) {
            this.block = block
        }

        override fun hide() {
            block = null
        }

        override fun goHome() {
            home++
        }
    }

    private val display = FakeDisplay()
    private var settings = ShortFormSettings(enabled = true)
    private var focus = false
    private lateinit var state: WellbeingStateRepository

    private fun TestScope.guard(): ShortFormGuard {
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("+01:00")

            override fun withZone(zone: ZoneId) = this

            override fun instant(): Instant = Instant.parse("2026-10-19T08:00:00Z").plusMillis(testScheduler.currentTime)
        }
        state = WellbeingStateRepository(
            DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            clock,
            logger,
            zone = clock::getZone
        )
        return ShortFormGuard({ settings }, { focus }, state, display, clock, backgroundScope, logger)
    }

    @Test
    fun f16_7_blockModeBlocksAtOnceAndOnlyInTheFrontApp() = runTest {
        val g = guard()
        g.onDetection(YT, true)
        runCurrent()
        assertNull("not in front yet", display.block)
        g.onForeground(YT)
        g.onDetection(YT, true)
        runCurrent()
        assertEquals(ShortFormBlock(YT, allowLeft = 1, autoHomeSeconds = null), display.block)
        assertEquals(1, state.shortFormToday().blocks)
        assertTrue(logged.any { it.startsWith("short videos: blocked $YT") })
    }

    @Test
    fun f16_18_leavingTheAppRemovesTheScreenButAShortGapDoesNot() = runTest {
        val g = guard()
        g.onForeground(YT)
        g.onDetection(YT, true)
        runCurrent()
        // The Short reloads underneath: no content for a moment, the screen stays.
        g.onDetection(YT, false)
        assertTrue(display.block != null)
        g.onForeground("com.android.launcher3")
        assertNull("in Recents or another app: gone", display.block)
    }

    @Test
    fun f16_10_goHomeThenQuietForASecond() = runTest {
        val g = guard()
        g.onForeground(YT)
        g.onDetection(YT, true)
        runCurrent()
        g.goHome()
        assertEquals(1, display.home)
        assertNull(display.block)
        g.onDetection(YT, true)
        runCurrent()
        assertNull("no flicker during the quiet second", display.block)
        advanceTimeBy(1_100)
        runCurrent()
        assertTrue("blocked again straight after", display.block != null)
    }

    @Test
    fun f16_8_limitModeBlocksWhenTheAllowanceIsUsedUp() = runTest {
        settings = ShortFormSettings(enabled = true, mode = ShortFormMode.LIMIT, allowanceMinutes = 15)
        val g = guard()
        g.onForeground(YT)
        g.onDetection(YT, true)
        advanceTimeBy(10 * MIN)
        runCurrent()
        assertNull(display.block)
        // Away for a while: 10 minutes are kept.
        g.onDetection(YT, false)
        runCurrent()
        assertEquals(10 * MIN, state.shortFormToday().millis)
        g.onDetection(YT, true)
        advanceTimeBy(5 * MIN - 1_000)
        runCurrent()
        assertNull(display.block)
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(display.block != null)
    }

    @Test
    fun f16_11_allowFiveMinutesThenBlockedAgainAndUsesRunOut() = runTest {
        val g = guard()
        g.onForeground(YT)
        g.onDetection(YT, true)
        runCurrent()
        g.allowFiveMinutes()
        runCurrent()
        assertNull(display.block)
        advanceTimeBy(5 * MIN + 1_000)
        runCurrent()
        assertEquals("the one use is spent", 0, display.block?.allowLeft)
    }

    @Test
    fun f16_16_focusAlwaysBlocksWithoutAllow() = runTest {
        settings = ShortFormSettings(enabled = true, mode = ShortFormMode.LIMIT, allowanceMinutes = 120)
        focus = true
        val g = guard()
        g.onForeground(YT)
        g.onDetection(YT, true)
        runCurrent()
        assertEquals("blocked despite the allowance, no Allow during focus", 0, display.block?.allowLeft)
    }

    @Test
    fun f16_1_f16_4_offOrAppNotChosenNeverBlocks() = runTest {
        settings = ShortFormSettings(enabled = false)
        val g = guard()
        g.onForeground(YT)
        g.onDetection(YT, true)
        runCurrent()
        assertNull(display.block)
        settings = ShortFormSettings(enabled = true, blockedApps = emptySet())
        g.onDetection(YT, true)
        runCurrent()
        assertNull(display.block)
    }
}
