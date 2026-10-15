package com.zeroos.core.logging

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.testing.MutableClock
import java.time.Duration
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticsPiecesTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = MutableClock()
    private val store by lazy { LogFileStore(temp.newFolder("logs"), clock) }
    private val settings by lazy {
        DiagnosticsSettings(context.getSharedPreferences("test", Context.MODE_PRIVATE), LogLevel.INFO)
    }

    @Test
    fun f17_11_settingsDefaultAndPersist() {
        assertTrue(settings.isLoggingEnabled)
        assertEquals(LogLevel.INFO, settings.minLevel)
        settings.isLoggingEnabled = false
        settings.minLevel = LogLevel.WARN
        val reread = DiagnosticsSettings(context.getSharedPreferences("test", Context.MODE_PRIVATE), LogLevel.INFO)
        assertFalse(reread.isLoggingEnabled)
        assertEquals(LogLevel.WARN, reread.minLevel)
    }

    @Test
    fun f17_9_parsesEntriesNewestFirstWithDetails() {
        val entries = parseLogLines(
            listOf(
                "2026-10-15T10:40:00.000+05:30 I/home: shown",
                "2026-10-15T10:41:00.000+05:30 E/apps: could not launch x",
                "    java.lang.SecurityException: gone",
                "    at a.b.C(C.kt:1)",
                "garbage without header"
            )
        )
        assertEquals(listOf("apps", "home"), entries.map { it.tag })
        assertEquals(LogLevel.ERROR, entries[0].level)
        assertEquals("could not launch x", entries[0].message)
        assertEquals(listOf("java.lang.SecurityException: gone", "at a.b.C(C.kt:1)", "garbage without header"), entries[0].details)
        assertEquals("2026-10-15T10:40:00.000+05:30", entries[1].time)
    }

    @Test
    fun f17_9_parsingIgnoresLinesBeforeFirstEntry() {
        assertTrue(parseLogLines(listOf("    orphan")).isEmpty())
    }

    @Test
    fun f17_9_readsAllFilesOldestFirst() {
        val small = LogFileStore(temp.newFolder("small"), clock, maxFileBytes = 12)
        listOf("line-1", "line-2", "line-3").forEach(small::append)
        assertEquals(listOf("line-1", "line-2", "line-3"), small.readAllLinesOldestFirst())
    }

    @Test
    fun f17_9_exportZipsLogsAndCrashReports() {
        store.append("a log line")
        store.writeCrashReport("a crash")
        val out = temp.newFolder("export")
        val zip = LogExporter(store, out, clock).export()
        val names = ZipFile(zip).use { file -> file.entries().toList().map { it.name } }
        assertTrue("log-0.txt" in names)
        assertTrue(names.any { it.startsWith(LogFileStore.CRASH_PREFIX) })

        clock.advanceBy(Duration.ofSeconds(1))
        LogExporter(store, out, clock).export()
        assertEquals("old exports are replaced", 1, out.listFiles()!!.size)
    }

    @Test
    fun f17_10_crashNoticeShowsEachNewReportOnce() {
        val notice = CrashNotice(store, settings)
        assertNull(notice.unseenReport())

        store.writeCrashReport("first")
        val report = notice.unseenReport()!!
        notice.markSeen(report)
        assertNull(notice.unseenReport())

        clock.advanceBy(Duration.ofSeconds(5))
        store.writeCrashReport("second")
        assertEquals("second", notice.unseenReport()!!.readText())
    }
}
