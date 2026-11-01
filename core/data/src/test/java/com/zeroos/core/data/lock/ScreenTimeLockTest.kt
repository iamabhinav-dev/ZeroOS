package com.zeroos.core.data.lock

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ScreenTimeLockSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val MINUTE = 60_000L

class ScreenTimeLockTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    /** Today's screen time the lock reads, in minutes; null = no Usage access. */
    private var usedMinutes: Long? = 0
    private val zone = ZoneId.of("+05:30")

    /** Virtual time drives both the timers and the clock; the test day starts at 20:00 local. */
    private fun TestScope.clock(): Clock = object : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId) = this

        override fun instant(): Instant = Instant.parse("2026-11-01T14:30:00Z").plusMillis(testScheduler.currentTime)
    }

    private suspend fun TestScope.lock(settings: ScreenTimeLockSettings): Pair<ScreenTimeLock, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(screenTimeLock = settings) }
        val repo = SettingsRepository(store, logger)
        val state = DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val lock = ScreenTimeLock(
            repo,
            { usedMinutes?.times(MINUTE) },
            LockUnlockStore(state),
            clock(),
            backgroundScope,
            logger
        ) { setOf("com.example.dialer", "com.example.settings", "com.zeroos.launcher") }
        lock.zone = { zone }
        return lock to repo
    }

    private val on =
        ScreenTimeLockSettings(enabled = true, capMinutes = 240, essentialApps = listOf("com.example.maps/com.example.maps.Main#0"))

    @Test
    fun f19_3_locksAtTheCapAndOnlyEssentialAndAlwaysAllowedAppsWork() = runTest {
        val (lock, _) = lock(on)
        usedMinutes = 239
        assertFalse(lock.refresh().active)
        usedMinutes = 240
        val state = lock.refresh()
        assertTrue(state.active)
        assertTrue(state.allowsKey("com.example.maps/com.example.maps.Main#0"))
        assertFalse(state.allowsKey("com.example.video/com.example.video.Main#0"))
        assertTrue("phone app always works", state.allowsPackage("com.example.dialer"))
        assertTrue("Android Settings never covered (F19.8)", state.allowsPackage("com.example.settings"))
        assertFalse(state.allowsPackage("com.example.video"))
        assertTrue(logged.contains("lock on: cap of 240 min reached"))
    }

    @Test
    fun f19_1_offByDefaultNeverLocks() = runTest {
        val (lock, _) = lock(ScreenTimeLockSettings())
        usedMinutes = 1_000
        val state = lock.refresh()
        assertFalse(state.active)
        assertTrue(state.allowsPackage("com.example.video"))
    }

    @Test
    fun f19_4_rightPhraseUnlocksForFifteenMinutesThenItComesBack() = runTest {
        val (lock, _) = lock(on)
        usedMinutes = 300
        assertTrue(lock.refresh().active)
        assertTrue(lock.unlock("IamWeakIneedThis"))
        assertFalse(lock.state.value.active)
        advanceTimeBy(15 * MINUTE - 1)
        runCurrent()
        assertFalse(lock.state.value.active)
        advanceTimeBy(2)
        runCurrent()
        assertTrue("locked again after 15 minutes", lock.state.value.active)
        assertTrue(logged.contains("lock: unlocked for 15 min"))
    }

    @Test
    fun f19_5_wrongPhraseChangesNothingAndIsNeverLogged() = runTest {
        val (lock, _) = lock(on)
        usedMinutes = 300
        lock.refresh()
        assertFalse(lock.unlock("iamweakineedthis"))
        assertFalse("case-sensitive", lock.unlock("IamWeakIneedThis "))
        assertTrue(lock.state.value.active)
        assertTrue(logged.contains("lock: wrong phrase"))
        assertTrue("typed text never logged (F17.8)", logged.none { it.contains("iamweak", ignoreCase = true) })
    }

    @Test
    fun f19_6_changedPhraseIsTheOneThatCounts() = runTest {
        val (lock, settings) = lock(on)
        settings.update("test") { it.copy(screenTimeLock = it.screenTimeLock.copy(phrase = "NotTodayPhone")) }
        assertFalse(lock.isPhrase("IamWeakIneedThis"))
        assertTrue(lock.isPhrase("NotTodayPhone"))
    }

    @Test
    fun f19_2_newDayEndsTheLockAtMidnight() = runTest {
        val (lock, _) = lock(on)
        usedMinutes = 300
        assertTrue(lock.refresh().active)
        // Midnight local is 4 h after the test's 20:00 start; the new day's screen time starts again.
        usedMinutes = 0
        advanceTimeBy(4 * 60 * MINUTE)
        runCurrent()
        assertFalse(lock.state.value.active)
    }

    @Test
    fun f19_7_withoutUsageAccessItDoesNotLock() = runTest {
        val (lock, _) = lock(on)
        usedMinutes = null
        assertFalse(lock.refresh().active)
        assertTrue(logged.contains("lock: screen time not readable; not locking"))
    }

    @Test
    fun f19_4_unlockSurvivesARestart() = runTest {
        val (lock, _) = lock(on)
        usedMinutes = 300
        lock.unlock("IamWeakIneedThis")
        assertEquals(lock.state.value.unlockedUntil, lock.refresh().unlockedUntil)
        assertFalse(lock.state.value.active)
    }
}
