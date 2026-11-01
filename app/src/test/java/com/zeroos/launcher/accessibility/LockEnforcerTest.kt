package com.zeroos.launcher.accessibility

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.LaunchableActivity
import com.zeroos.core.data.apps.LauncherAppsSource
import com.zeroos.core.data.lock.LockUnlockStore
import com.zeroos.core.data.lock.ScreenTimeLock
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStateSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.ScreenTimeLockSettings
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

private const val MINUTE = 60_000L
private const val OWN = "com.zeroos.launcher"
private const val HOME_ACTIVITY = "com.zeroos.launcher.MainActivity"

@OptIn(ExperimentalCoroutinesApi::class)
class LockEnforcerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val source = object : LauncherAppsSource {
        override fun launchableActivities() = listOf("Video", "Maps", "Dialer").map {
            LaunchableActivity(AppKey("com.example.${it.lowercase()}", "Main", 0), it)
        }

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

    private val display = FakeDisplay()
    private val unlockAsked = mutableListOf<String>()

    /** Today's screen time so far: grows with virtual time while a test runs. */
    private var usedAtStart = 0L

    private fun TestScope.clock(): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("+05:30")

        override fun withZone(zone: ZoneId) = this

        override fun instant(): Instant = Instant.parse("2026-11-01T08:00:00Z").plusMillis(testScheduler.currentTime)
    }

    private suspend fun TestScope.enforcer(lock: ScreenTimeLockSettings): LockEnforcer {
        val io = StandardTestDispatcher(testScheduler)
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        store.updateData { LauncherSettings(screenTimeLock = lock) }
        val settings = SettingsRepository(store, logger)
        val clock = clock()
        val stateStore = DataStoreFactory.create(WellbeingStateSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val state = WellbeingStateRepository(stateStore, clock, logger, zone = clock::getZone)
        val screenTimeLock = ScreenTimeLock(
            settings,
            { usedAtStart + testScheduler.currentTime },
            LockUnlockStore(stateStore),
            clock,
            backgroundScope,
            logger
        ) { setOf(OWN, "com.example.dialer") }
        val apps = AppsRepository(source, logger, io)
        val factory = OverlayContentFactory(settings, apps, state, OverlayPunSizes(20, 20, 15, lock = 12))
        return LockEnforcer(
            screenTimeLock,
            { setOf("com.example.video", "com.example.maps", "com.example.dialer") },
            factory,
            display,
            backgroundScope,
            logger,
            LockTarget(OWN, HOME_ACTIVITY, clock) { unlockAsked += it }
        )
    }

    private val lockOn = ScreenTimeLockSettings(enabled = true, capMinutes = 60, essentialApps = listOf("com.example.maps/Main#0"))

    @Test
    fun f19_3_aNonEssentialAppGetsTheLockScreenOnceTheCapIsReached() = runTest {
        usedAtStart = 61 * MINUTE
        val e = enforcer(lockOn)
        e.onWindow("com.example.video", "Main")
        runCurrent()
        val shown = display.content!!
        assertEquals(Overlay.Lock("com.example.video", 61 * MINUTE), shown.overlay)
        assertEquals("Video", shown.app.label)
        assertTrue(shown.pun in 0 until 12)
        assertTrue(logged.contains("lock screen over com.example.video"))
    }

    @Test
    fun f19_3_essentialPhoneAndSystemWindowsAreNeverCovered() = runTest {
        usedAtStart = 61 * MINUTE
        val e = enforcer(lockOn)
        e.onWindow("com.example.maps", "Main")
        runCurrent()
        assertNull(display.content)
        e.onWindow("com.example.dialer", "Main")
        runCurrent()
        assertNull("the phone app always works", display.content)
        e.onWindow("com.android.systemui", "Shade")
        runCurrent()
        assertNull("not a launchable app: system UI, keyboards", display.content)
    }

    @Test
    fun f19_3_comesOnWhileTheAppIsInFrontWhenTheCapIsReached() = runTest {
        usedAtStart = 50 * MINUTE
        val e = enforcer(lockOn)
        e.onWindow("com.example.video", "Main")
        runCurrent()
        assertNull("10 minutes left", display.content)
        advanceTimeBy(10 * MINUTE - 1_000)
        runCurrent()
        assertNull(display.content)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals("com.example.video", display.showingFor)
    }

    @Test
    fun f19_4_unlockSendsThePersonToZeroOsAndGoHomeLeaves() = runTest {
        usedAtStart = 61 * MINUTE
        val e = enforcer(lockOn)
        e.onWindow("com.example.video", "Main")
        runCurrent()
        display.actions!!.onUnlock()
        assertEquals(listOf("com.example.video"), unlockAsked)
        assertNull(display.content)
        e.onWindow("com.example.video", "Main")
        runCurrent()
        display.actions!!.onGoHome()
        assertEquals(1, display.wentHome)
        assertNull(display.content)
    }

    @Test
    fun f19_3_zeroOsHomeTakesTheLockScreenAwayButItsOwnOverlayDoesNot() = runTest {
        usedAtStart = 61 * MINUTE
        val e = enforcer(lockOn)
        e.onWindow("com.example.video", "Main")
        runCurrent()
        e.onWindow(OWN, "android.widget.FrameLayout")
        runCurrent()
        assertEquals("our own overlay window isn't an app in front", "com.example.video", display.showingFor)
        e.onWindow(OWN, HOME_ACTIVITY)
        assertNull(display.content)
    }

    @Test
    fun f19_1_lockOffNeverCovers() = runTest {
        usedAtStart = 600 * MINUTE
        val e = enforcer(ScreenTimeLockSettings())
        e.onWindow("com.example.video", "Main")
        runCurrent()
        assertNull(display.content)
    }
}
