package com.zeroos.launcher.accessibility

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.OutsideWatcher
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.data.wellbeing.UsageEvent
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.WellbeingSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

private const val M = 60_000L
private const val HOME = "com.zeroos.launcher.MainActivity"

@OptIn(ExperimentalCoroutinesApi::class)
class OutsideEnforcerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf(LaunchableActivity(AppKey("yt", "Main", 0), "YouTube"))

        override fun registerChangeListener(onChange: (String) -> Unit) = LauncherAppsSource.ChangeListenerHandle {}

        override fun openAppInfo(key: AppKey) = Unit

        override fun uninstall(key: AppKey) = Unit

        override fun launch(key: AppKey) = Unit
    }

    private class FakeDisplay : OverlayDisplay {
        var content: OverlayContent? = null
        var actions: OverlayActions? = null
        var wentHome = 0
        override val showingFor get() = content?.overlay?.packageName

        override fun show(content: OverlayContent, actions: OverlayActions) {
            this.content = content
            this.actions = actions
        }

        override fun hide() {
            content = null
        }

        override fun goHome() {
            wentHome++
        }
    }

    private val events = mutableListOf<UsageEvent>()
    private val display = FakeDisplay()

    /** Virtual time drives both the timers and the clock. */
    private fun TestScope.clock(): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("Asia/Kolkata")

        override fun withZone(zone: ZoneId) = this

        override fun instant(): Instant = Instant.parse("2026-10-18T04:00:00Z").plusMillis(testScheduler.currentTime)
    }

    private suspend fun TestScope.enforcer(wellbeing: WellbeingSettings): OutsideEnforcer {
        val io = StandardTestDispatcher(testScheduler)
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(wellbeing = wellbeing) }
        val settings = SettingsRepository(store, logger)
        val clock = clock()
        val state = WellbeingStateRepository(
            DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = { temp.newFile().apply { delete() } }),
            clock,
            logger,
            zone = clock::getZone
        )
        val watcher = OutsideWatcher(settings, { _, _ -> events.toList() }, state, clock, io, logger, clock::getZone)
        val factory = OverlayContentFactory(settings, AppsRepository(source, logger, io), state, OverlayPunSizes(20, 20, 15))
        return OutsideEnforcer(watcher, factory, display, backgroundScope, logger, "com.zeroos.launcher", HOME)
    }

    private fun TestScope.open(pkg: String) = events.add(UsageEvent(pkg, UsageEvent.Type.RESUMED, clock().millis()))

    @Test
    fun f8_16_breakAfterFifteenMinutesThenNextOneFifteenMinutesAfterIt() = runTest {
        val e = enforcer(WellbeingSettings(enabled = true, breaksEnabled = true, apps = mapOf("yt" to AppRule(takeBreak = true))))
        open("yt")
        e.onWindow("yt", "Main")
        advanceTimeBy(15 * M - 1_000)
        runCurrent()
        assertNull("not yet", display.content)
        advanceTimeBy(2_000)
        runCurrent()
        val shown = display.content!!
        assertEquals(Overlay.Break("yt", 5), shown.overlay)
        assertEquals("YouTube", shown.app.label)
        assertTrue(shown.pun in 0 until 20 && shown.buttonPun in 0 until 15)

        display.actions!!.onBreakDone()
        runCurrent()
        assertNull(display.content)
        advanceTimeBy(15 * M - 1_000)
        runCurrent()
        assertNull("next break counts from the end of this one", display.content)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(Overlay.Break("yt", 5), display.content?.overlay)
        assertTrue(logged.contains("break screen over yt"))
    }

    @Test
    fun f8_7_limitOutsideBlocksAndGoHomeLeavesTheApp() = runTest {
        val e = enforcer(
            WellbeingSettings(enabled = true, outsideEnforcement = true, apps = mapOf("yt" to AppRule(dailyLimitMinutes = 5)))
        )
        open("yt")
        e.onWindow("yt", "Main")
        // The limit runs out while the app is open.
        advanceTimeBy(5 * M + 1_000)
        runCurrent()
        assertEquals(Overlay.Limit("yt", 5, 2), display.content?.overlay)

        display.actions!!.onGoHome()
        assertEquals(1, display.wentHome)
        assertNull(display.content)
        assertTrue(logged.contains("outside: went home from yt"))
    }

    @Test
    fun f8_4_fiveMoreMinutesOutsideThenBlockedAgain() = runTest {
        val e = enforcer(
            WellbeingSettings(enabled = true, outsideEnforcement = true, apps = mapOf("yt" to AppRule(dailyLimitMinutes = 5)))
        )
        open("yt")
        advanceTimeBy(6 * M)
        e.onWindow("yt", "Main")
        runCurrent()
        display.actions!!.onMoreMinutes()
        runCurrent()
        assertNull(display.content)
        // The limit is now 10 min and 6 are used: blocked again 4 min later.
        advanceTimeBy(4 * M - 1_000)
        runCurrent()
        assertNull(display.content)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(Overlay.Limit("yt", 10, 1), display.content?.overlay)
    }

    @Test
    fun f8_17_leavingForHomeClosesTheOverlayAndStopsTheTimer() = runTest {
        val e = enforcer(WellbeingSettings(enabled = true, breaksEnabled = true, apps = mapOf("yt" to AppRule(takeBreak = true))))
        open("yt")
        e.onWindow("yt", "Main")
        advanceTimeBy(5 * M)
        events.add(UsageEvent("yt", UsageEvent.Type.PAUSED, clock().millis()))
        e.onWindow("com.zeroos.launcher", HOME)
        advanceTimeBy(30 * M)
        runCurrent()
        assertNull(display.content)
        // ZeroOS's own overlay window isn't "home": it changes nothing.
        e.onWindow("com.zeroos.launcher", "android.widget.FrameLayout")
        assertNull(display.content)
    }

    @Test
    fun f8_16_screenBackOnChecksTheAppInFront() = runTest {
        val e = enforcer(WellbeingSettings(enabled = true, breaksEnabled = true, apps = mapOf("yt" to AppRule(takeBreak = true))))
        open("yt")
        e.onScreenOff()
        advanceTimeBy(20 * M)
        e.onScreenOn()
        runCurrent()
        assertEquals(Overlay.Break("yt", 5), display.content?.overlay)
    }
}
