package com.zeroos.launcher.diagnostics

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.StoredLogEntry
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiagnosticsScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val state = DiagnosticsUiState(
        entries = listOf(
            StoredLogEntry("2026-10-15T10:42:03.123+05:30", LogLevel.INFO, "home", "shown", emptyList()),
            StoredLogEntry("2026-10-15T10:41:58.000+05:30", LogLevel.INFO, "apps", "launched com.whatsapp", emptyList()),
            StoredLogEntry(
                "2026-10-15T10:40:12.000+05:30",
                LogLevel.ERROR,
                "apps",
                "could not launch x",
                listOf("java.lang.SecurityException: gone")
            )
        ),
        crashReports = listOf(CrashReportItem("crash-1.txt", "java.lang.IllegalStateException: boom")),
        lastStop = "killed by the system or phone maker",
        level = LogLevel.INFO
    )
    private val calls = mutableListOf<String>()

    private fun show(debug: Boolean = false, shown: DiagnosticsUiState = state) = compose.setContent {
        ZeroTheme {
            DiagnosticsScreen(
                state = shown,
                showDebugTools = debug,
                onFilter = { calls += "filter:$it" },
                onOpenReports = { calls += "reports" },
                onExport = { calls += "export" },
                onClear = { calls += "clear" },
                onToggleLogging = { calls += "toggle" },
                onCycleLevel = { calls += "level" },
                onCrashNow = { calls += "crash" },
                onToggleCrashOnStart = { calls += "crashOnStart" },
                onBack = { calls += "back" }
            )
        }
    }

    /** Lazy lists only compose visible rows, so scroll to a row before using it. */
    private fun tap(text: String) {
        compose.onNodeWithTag(DiagnosticsTestTags.ROOT).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }

    private fun assertShown(text: String) {
        compose.onNodeWithTag(DiagnosticsTestTags.ROOT).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertExists()
    }

    @Test
    fun f17_9_showsLogsCrashReportsAndLastStop() {
        show()
        assertShown("10:42:03  I  home  shown")
        assertShown("Crash reports (1)")
        assertShown("Last stop: killed by the system or phone maker")
    }

    @Test
    fun f17_9_tapErrorShowsStackTrace() {
        show()
        compose.onNodeWithText("java.lang.SecurityException: gone").assertDoesNotExist()
        compose.onNodeWithText("10:40:12  E  apps  could not launch x").performClick()
        compose.onNodeWithText("java.lang.SecurityException: gone").assertExists()
    }

    @Test
    fun f17_9_actionsCallBack() {
        show()
        tap("Warn")
        tap("Export logs")
        tap("Logging")
        tap("Level")
        tap("Crash reports (1)")
        assertEquals(listOf("filter:WARN", "export", "toggle", "level", "reports"), calls)
    }

    @Test
    fun f17_9_clearAsksForConfirmation() {
        show()
        tap("Clear logs")
        compose.onNodeWithText("Clear all logs?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        tap("Clear logs")
        compose.onNodeWithText("All log files and crash reports will be deleted.").assertExists()
        compose.onAllNodes(hasText("Clear logs"))[1].performClick()
        assertEquals(listOf("clear"), calls)
    }

    @Test
    fun step1_4_crashButtonOnlyInDebug() {
        show(debug = false)
        assertShown("Level")
        compose.onNodeWithText("Crash now (debug only)").assertDoesNotExist()
    }

    @Test
    fun step1_4_crashButtonShownInDebug() {
        show(debug = true)
        assertShown("Crash now (debug only)")
    }

    @Test
    fun step1_5_crashOnStartToggleInDebug() {
        show(debug = true)
        tap("Crash on start (debug only)")
        assertEquals(listOf("crashOnStart"), calls)
    }

    @Test
    fun step1_4_diagnosticsScreenshot() {
        show(debug = true)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f17_10_crashBannerButtons() {
        compose.setContent { ZeroTheme { CrashBanner(onView = { calls += "view" }, onDismiss = { calls += "dismiss" }) } }
        compose.onNodeWithText("ZeroOS crashed last time.").assertExists()
        compose.onNodeWithText("View report").performClick()
        compose.onNodeWithText("Dismiss").performClick()
        assertEquals(listOf("view", "dismiss"), calls)
    }

    @Test
    fun f17_9_crashReportsListAndReport() {
        compose.setContent {
            ZeroTheme {
                CrashReportsScreen(
                    reports = state.crashReports,
                    onOpen = { calls += it.fileName },
                    onBack = { calls += "back" }
                )
            }
        }
        compose.onNodeWithText("java.lang.IllegalStateException: boom").performClick()
        assertEquals(listOf("crash-1.txt"), calls)
    }

    @Test
    fun nfrP4_memoryLine() {
        show(shown = state.copy(memoryMb = 38))
        compose.onNodeWithTag(DiagnosticsTestTags.ROOT).performScrollToNode(hasText("Memory: 38 MB"))
        compose.onNodeWithText("Memory: 38 MB").assertExists()
    }
}
