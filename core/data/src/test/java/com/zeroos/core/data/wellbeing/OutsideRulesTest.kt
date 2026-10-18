package com.zeroos.core.data.wellbeing

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.UsageEvent.Type.PAUSED
import com.zeroos.core.data.wellbeing.UsageEvent.Type.RESUMED
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.testing.MutableClock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val M = 60_000L

private fun e(pkg: String, type: UsageEvent.Type, minute: Long, plusMillis: Long = 0) = UsageEvent(pkg, type, minute * M + plusMillis)

class OutsideRulesTest {
    private val rules = mapOf(
        "yt" to AppRule(takeBreak = true),
        "ig" to AppRule(dailyLimitMinutes = 30, takeBreak = true),
        "x" to AppRule(dailyLimitMinutes = 30)
    )
    private val on = WellbeingSettings(enabled = true, outsideEnforcement = true, breaksEnabled = true, apps = rules)

    private fun facts(events: List<UsageEvent>, now: Long, extras: Int = 0, lastBreak: Long? = null, knownInFront: Boolean = false) =
        OutsideFacts(events, 0, 0, now * M, extras, lastBreak?.times(M), knownInFront)

    @Test
    fun f8_17_shortGapsKeepTheSessionLongGapsResetIt() {
        // Activity switch inside the app, and a 30 s reply elsewhere: one session from minute 0.
        val short =
            listOf(e("yt", RESUMED, 0), e("yt", PAUSED, 5), e("yt", RESUMED, 5, 50), e("yt", PAUSED, 8), e("yt", RESUMED, 8, 30_000))
        assertEquals(0L, currentSessionStart(short, "yt", 0, 20 * M))
        // Away for 2 minutes: a new session from minute 10.
        val long = listOf(e("yt", RESUMED, 0), e("yt", PAUSED, 8), e("yt", RESUMED, 10))
        assertEquals(10 * M, currentSessionStart(long, "yt", 0, 20 * M))
        // Not in front now.
        assertNull(currentSessionStart(listOf(e("yt", RESUMED, 0), e("yt", PAUSED, 8)), "yt", 0, 20 * M))
        // Just came to the front but Android hasn't recorded it yet.
        assertEquals(20 * M, currentSessionStart(emptyList(), "yt", 0, 20 * M, knownInFront = true))
    }

    @Test
    fun f8_16_breakDueAfterFifteenMinutesStraightOtherwiseTimerForTheRest() {
        val open = listOf(e("yt", RESUMED, 0))
        assertEquals(OutsideCheck(checkAgainInMillis = 5 * M), on.outsideCheck("yt", facts(open, now = 10)))
        assertEquals(Overlay.Break("yt", 5), on.outsideCheck("yt", facts(open, now = 15)).overlay)
        // After a break the next one is 15 minutes later.
        assertEquals(OutsideCheck(checkAgainInMillis = 10 * M), on.outsideCheck("yt", facts(open, now = 20, lastBreak = 15)))
    }

    @Test
    fun f8_7_usedUpLimitBlocksOutsideAndWinsOverTheBreak() {
        val open = listOf(e("ig", RESUMED, 0))
        assertEquals(Overlay.Limit("ig", 30, 2), on.outsideCheck("ig", facts(open, now = 30)).overlay)
        // Under the limit: the nearer of limit (20 min left) and break (5 min left).
        assertEquals(OutsideCheck(checkAgainInMillis = 5 * M), on.outsideCheck("ig", facts(open, now = 10)))
        // "5 more minutes" used: the limit is 35 today.
        assertEquals(
            OutsideCheck(checkAgainInMillis = 5 * M),
            on.outsideCheck(
                "x",
                facts(
                    open.map {
                        it.copy(packageName = "x")
                    },
                    30,
                    extras = 1
                )
            )
        )
    }

    @Test
    fun f8_7_f8_8_partsOffMeanNothingOutside() {
        val open = listOf(e("ig", RESUMED, 0))
        assertEquals(OutsideCheck(), on.copy(enabled = false).outsideCheck("ig", facts(open, 60)))
        assertEquals(OutsideCheck(), on.outsideCheck("maps", facts(listOf(e("maps", RESUMED, 0)), 60)))
        // Outside off: no limit block, only the break.
        assertEquals(Overlay.Break("ig", 5), on.copy(outsideEnforcement = false).outsideCheck("ig", facts(open, 30)).overlay)
        // Not in front: nothing, no timer.
        assertEquals(OutsideCheck(), on.outsideCheck("ig", facts(listOf(e("ig", RESUMED, 0), e("ig", PAUSED, 5)), 30)))
    }

    @Test
    fun f8_18_breakLengthsFromSettings() {
        val custom = on.copy(breakAfterMinutes = 30, breakWaitSeconds = 10)
        assertEquals(Overlay.Break("yt", 10), custom.outsideCheck("yt", facts(listOf(e("yt", RESUMED, 0)), 30)).overlay)
    }

    @Test
    fun watchedPackagesFollowTheParts() {
        assertEquals(setOf("yt", "ig", "x"), on.watchedPackages)
        assertEquals(setOf("yt", "ig"), on.copy(outsideEnforcement = false).watchedPackages)
        assertEquals(setOf("ig", "x"), on.copy(breaksEnabled = false).watchedPackages)
        assertEquals(emptySet<String>(), on.copy(enabled = false).watchedPackages)
    }
}

class OutsideWatcherTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Test
    fun f8_16_watcherChecksTheAppInFrontAndCountsFromTheLastBreak() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-18T10:00:00Z"))
        val start = clock.millis()
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData {
            LauncherSettings(
                wellbeing = WellbeingSettings(enabled = true, breaksEnabled = true, apps = mapOf("yt" to AppRule(takeBreak = true)))
            )
        }
        val state = WellbeingStateRepository(
            DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            clock,
            logger,
            zone = clock::getZone
        )
        val watcher = OutsideWatcher(
            SettingsRepository(store, logger),
            { _, _ -> listOf(UsageEvent("yt", RESUMED, start)) },
            state,
            clock,
            UnconfinedTestDispatcher(testScheduler),
            logger,
            clock::getZone
        )
        assertEquals("yt", watcher.appInFront())
        assertEquals(OutsideCheck(checkAgainInMillis = 15 * M), watcher.check("yt", knownInFront = true))
        clock.advanceBy(Duration.ofMinutes(15))
        assertEquals(Overlay.Break("yt", 5), watcher.check("yt", knownInFront = false).overlay)
        watcher.breakTaken("yt")
        assertEquals(OutsideCheck(checkAgainInMillis = 15 * M), watcher.check("yt", knownInFront = false))
        assertEquals(OutsideCheck(), watcher.check("maps", knownInFront = true))
    }
}
