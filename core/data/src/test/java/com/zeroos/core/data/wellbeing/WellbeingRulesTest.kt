package com.zeroos.core.data.wellbeing

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.focus.FocusStateSerializer
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.testing.MutableClock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val MIN = 60_000L

class StopRulesTest {
    private val on = WellbeingSettings(
        enabled = true,
        apps = mapOf(
            "ig" to AppRule(pause = true, dailyLimitMinutes = 30),
            "yt" to AppRule(dailyLimitMinutes = 60),
            "x" to AppRule(pause = true)
        )
    )

    @Test
    fun f8_8_masterOffStopsNothing() {
        val off = on.copy(enabled = false)
        assertFalse(off.mayStop("ig"))
        assertNull(off.stopFor("ig", 999 * MIN, 0))
        assertTrue(off.minutesLeft(mapOf("ig" to MIN), emptyMap()).isEmpty())
    }

    @Test
    fun f8_1_pauseForFlaggedAppsOnly() {
        assertEquals(StopScreen.Pause(5, false), on.stopFor("x", 0, 0))
        assertEquals(StopScreen.Pause(7, true), on.copy(pauseSeconds = 7, intentionPrompt = true).stopFor("ig", 0, 0))
        assertFalse(on.mayStop("maps"))
        assertNull(on.copy(pauseEnabled = false).stopFor("x", 0, 0))
    }

    @Test
    fun f8_4_usedUpLimitBlocksAndWinsOverThePause() {
        assertEquals(StopScreen.Limit(30, 2), on.stopFor("ig", 30 * MIN, 0))
        // Under the limit: the pause still shows; a limit-only app just opens.
        assertEquals(StopScreen.Pause(5, false), on.stopFor("ig", 29 * MIN, 0))
        assertNull(on.stopFor("yt", 59 * MIN, 0))
    }

    @Test
    fun f8_4_fiveMoreMinutesExtendsTheLimitUpToTheCap() {
        assertNull(on.stopFor("yt", 64 * MIN, 1))
        assertEquals(StopScreen.Limit(65, 1), on.stopFor("yt", 65 * MIN, 1))
        // Uses past the cap never show a negative count.
        assertEquals(StopScreen.Limit(80, 0), on.stopFor("yt", 80 * MIN, 3))
    }

    @Test
    fun f8_9_limitsOffIgnoresLimits() {
        val noLimits = on.copy(limitsEnabled = false)
        assertNull(noLimits.stopFor("yt", 999 * MIN, 0))
        assertEquals(StopScreen.Pause(5, false), noLimits.stopFor("ig", 999 * MIN, 0))
    }

    @Test
    fun f8_5_minutesLeftRoundsUpAndStopsAtZero() {
        val left = on.minutesLeft(mapOf("ig" to 12 * MIN + 1, "yt" to 90 * MIN), mapOf("yt" to 1))
        assertEquals(mapOf("ig" to 18, "yt" to 0), left)
    }
}

class PunBagTest {
    @Test
    fun f8_13_everyLineOnceBeforeAnyRepeatAndNeverTwiceInARow() {
        val random = Random(42)
        var bag = PunBag()
        val drawn = mutableListOf<Int>()
        repeat(20 * 50) {
            val (line, next) = bag.draw(20, random)
            drawn += line
            bag = next
        }
        drawn.chunked(20).forEach { assertEquals((0 until 20).toSet(), it.toSet()) }
        drawn.zipWithNext().forEach { (a, b) -> assertTrue("repeat $a", a != b) }
    }

    @Test
    fun f8_13_positionSurvivesARestartAndAChangedListStartsOver() {
        val (first, bag) = PunBag().draw(20, Random(1))
        // The saved bag continues where it left off.
        val (second, _) = bag.copy().draw(20, Random(2))
        assertEquals(bag.order[1], second)
        assertTrue(first != second)
        // A list with a different length starts a fresh shuffle.
        val (_, resized) = bag.draw(5, Random(3))
        assertEquals(5, resized.order.size)
    }
}

class WellbeingStateRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    private fun TestScope.repository(clock: MutableClock) = WellbeingStateRepository(
        DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
        clock,
        logger,
        Random(7),
        clock::getZone
    )

    @Test
    fun f8_6_extraUsesResetAtLocalMidnight() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-17T22:30:00Z")) // 23:30 local (+01:00)
        val repo = repository(clock)
        repo.useExtra("yt")
        repo.useExtra("yt")
        assertEquals(mapOf("yt" to 2), repo.extrasUsedToday())
        clock.advanceBy(Duration.ofMinutes(31)) // 00:01 the next day
        assertEquals(emptyMap<String, Int>(), repo.extrasUsedToday())
        repo.useExtra("yt")
        assertEquals(mapOf("yt" to 1), repo.extrasUsedToday())
    }

    @Test
    fun f8_6_timeZoneAndClockChangesFollowTheLocalDate() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-17T10:00:00Z"))
        val repo = repository(clock)
        repo.useExtra("yt")
        // Flying west: still the 17th locally, so the uses stay.
        clock.setZone(ZoneId.of("America/New_York"))
        assertEquals(mapOf("yt" to 1), repo.extrasUsedToday())
        // Clock set back a day by hand: a different day, so nothing counts.
        clock.setTo(Instant.parse("2026-10-16T10:00:00Z"))
        assertEquals(emptyMap<String, Int>(), repo.extrasUsedToday())
    }

    @Test
    fun f8_13_punListsHaveSeparateBags() = runTest {
        val repo = repository(MutableClock())
        val pause = List(20) { repo.nextPun(PunList.PAUSE, 20) }
        val limit = List(20) { repo.nextPun(PunList.LIMIT, 20) }
        assertEquals((0 until 20).toSet(), pause.toSet())
        assertEquals((0 until 20).toSet(), limit.toSet())
        val breaks = List(20) { repo.nextPun(PunList.BREAK, 20) }
        val buttons = List(15) { repo.nextPun(PunList.BREAK_BUTTON, 15) }
        assertEquals((0 until 20).toSet(), breaks.toSet())
        assertEquals((0 until 15).toSet(), buttons.toSet())
    }
}

class LaunchGateTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val launched = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = emptyList<LaunchableActivity>()

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) {
            launched += key.packageName
        }
    }

    private fun app(pkg: String) = AppEntry(AppKey(pkg, "Main", 0), pkg)

    private lateinit var focus: FocusRepository

    private fun TestScope.focusRepository() = FocusRepository(
        DataStoreFactory.create(FocusStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
        Clock.systemUTC(),
        logger
    ).also { focus = it }

    @Test
    fun f9_3_duringFocusOnlyAllowedAppsOpen() = runTest(UnconfinedTestDispatcher()) {
        val gate = gate(LauncherSettings())
        focus.start(25, setOf(app("phone").key.asString()))
        gate.focus.first { it != null }
        assertFalse(gate.launch(app("yt")))
        assertTrue(gate.launch(app("phone")))
        assertEquals(listOf("phone"), launched)
        gate.endFocus()
        gate.focus.first { it == null }
        assertTrue(gate.launch(app("yt")))
    }

    private suspend fun TestScope.gate(settings: LauncherSettings): LaunchGate {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { settings }
        val repo = SettingsRepository(store, logger)
        repo.settings.first()
        return LaunchGate(
            AppsRepository(source, logger, UnconfinedTestDispatcher(testScheduler)),
            repo,
            focusRepository(),
            backgroundScope,
            logger
        )
    }

    @Test
    fun f8_1_appsWithARuleWaitForTheStopScreenOthersOpenAtOnce() = runTest(UnconfinedTestDispatcher()) {
        val gate = gate(LauncherSettings(wellbeing = WellbeingSettings(enabled = true, apps = mapOf("ig" to AppRule(pause = true)))))
        assertTrue(gate.launch(app("maps")))
        assertEquals(listOf("maps"), launched)
        assertTrue(gate.launch(app("ig")))
        assertEquals(app("ig"), gate.stopFor.value)
        assertEquals(listOf("maps"), launched)
        assertTrue(gate.open(app("ig")))
        assertNull(gate.stopFor.value)
        assertEquals(listOf("maps", "ig"), launched)
    }

    @Test
    fun f8_8_featureOffOpensEverythingAndDismissKeepsItClosed() = runTest(UnconfinedTestDispatcher()) {
        val rules = mapOf("ig" to AppRule(pause = true))
        val off = gate(LauncherSettings(wellbeing = WellbeingSettings(enabled = false, apps = rules)))
        off.launch(app("ig"))
        assertEquals(listOf("ig"), launched)

        val on = gate(LauncherSettings(wellbeing = WellbeingSettings(enabled = true, apps = rules)))
        on.launch(app("ig"))
        on.dismiss()
        assertNull(on.stopFor.value)
        assertEquals(listOf("ig"), launched)
    }
}

class WellbeingStatusTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Test
    fun f8_8_noUsageReadWithoutAnActiveLimit() = runTest {
        var reads = 0
        val clock = MutableClock(Instant.parse("2026-10-17T10:00:00Z"))
        val source = UsageEventSource { _, _ ->
            reads++
            listOf(UsageEvent("yt", UsageEvent.Type.RESUMED, Instant.parse("2026-10-17T09:00:00Z").toEpochMilli()))
        }
        val state = WellbeingStateRepository(
            DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            clock,
            logger,
            zone = clock::getZone
        )
        val status = WellbeingStatus(TodayUsage(source, clock, UnconfinedTestDispatcher(testScheduler), logger, clock::getZone), state)
        val pauseOnly = WellbeingSettings(enabled = true, apps = mapOf("yt" to AppRule(pause = true)))
        assertEquals(emptyMap<String, Int>(), status.minutesLeft(pauseOnly))
        assertEquals(StopScreen.Pause(5, false), status.stopFor(pauseOnly, "yt"))
        assertEquals(0, reads)

        val limited = pauseOnly.copy(apps = mapOf("yt" to AppRule(dailyLimitMinutes = 30)))
        assertEquals(StopScreen.Limit(60, 2), status.stopFor(limited, "yt"))
        assertEquals(mapOf("yt" to 0), status.minutesLeft(limited))
        assertEquals(2, reads)
    }

    @Test
    fun f8_3_usageAccessRemovedMeansNoLimitsNotACrash() = runTest {
        val clock = MutableClock()
        val usage =
            TodayUsage({ _, _ ->
                throw SecurityException("no access")
            }, clock, UnconfinedTestDispatcher(testScheduler), logger, clock::getZone)
        assertEquals(emptyMap<String, Long>(), usage.foregroundToday())
    }
}
