package com.zeroos.launcher.safemode

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.feature.home.HomeTestTags
import com.zeroos.launcher.MainActivity
import com.zeroos.launcher.OnboardingDoneRule
import com.zeroos.launcher.ZeroApplication
import com.zeroos.launcher.diagnostics.DebugTestCrash
import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** F18 end to end: after 3 early crashes the activity opens in safe mode; the debug crash switch works. */
@RunWith(AndroidJUnit4::class)
class SafeModeActivityTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val onboardingDone = OnboardingDoneRule()

    private val app = ApplicationProvider.getApplicationContext<ZeroApplication>()

    @After
    fun tearDown() {
        app.crashLoopGuard.exitSafeMode()
        app.diagnosticsSettings.crashOnStart = false
    }

    /** Simulates three crashes soon after start, then a fresh process start. */
    private fun recordThreeEarlyCrashes() {
        File(app.filesDir, "safe_mode").mkdirs()
        repeat(3) { app.crashLoopGuard.recordCrash() }
        app.crashLoopGuard.onProcessStart()
    }

    @Test
    fun f18_2_opensInSafeModeAfterThreeEarlyCrashes() {
        recordThreeEarlyCrashes()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag(SafeModeTestTags.ROOT).assertExists()
            compose.onNodeWithTag(HomeTestTags.ROOT).assertDoesNotExist()
        }
    }

    @Test
    fun f18_6_normalStartWithoutCrashes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Home appears once settings have loaded (onboarding gate, F14).
            compose.waitUntil(WAIT_MS) { compose.onAllNodesWithTag(HomeTestTags.ROOT).fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun step1_5_debugSwitchCrashesOnStart() {
        app.diagnosticsSettings.crashOnStart = true
        val result = runCatching { ActivityScenario.launch(MainActivity::class.java).close() }
        assertTrue(generateSequence(result.exceptionOrNull()) { it.cause }.any { it is DebugTestCrash })
    }

    @Test
    fun step1_5_safeModeClearsDebugSwitch() {
        app.diagnosticsSettings.crashOnStart = true
        recordThreeEarlyCrashes()
        app.onCreate()
        assertFalse(app.diagnosticsSettings.crashOnStart)
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
