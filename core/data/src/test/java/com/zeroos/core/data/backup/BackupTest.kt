package com.zeroos.core.data.backup

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.testing.MutableClock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = MutableClock()
    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val custom = LauncherSettings(
        favorites = listOf("a/a.Main#0"),
        hiddenApps = setOf("b/b.Main#0"),
        renamedApps = mapOf("a/a.Main#0" to "Time Sink"),
        home = HomeSettings(alignment = Alignment.CENTER, maxFavorites = 4)
    )

    private fun kotlinx.coroutines.test.TestScope.repository(diagnostics: DiagnosticsSettings): Pair<BackupRepository, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        return BackupRepository(
            settings,
            diagnostics,
            context.getSharedPreferences("backup-test-${temp.root.name}", Context.MODE_PRIVATE),
            clock,
            logger,
            AppVersion("0.0.30", 30)
        ) to
            settings
    }

    private fun diagnostics(name: String) = DiagnosticsSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE), LogLevel.INFO)

    @Test
    fun f13_1_f13_2_exportThenImportRestoresEverything() = runTest {
        val sourceDiagnostics = diagnostics("diag-a").apply {
            isLoggingEnabled = false
            minLevel = LogLevel.WARN
        }
        val (source, sourceSettings) = repository(sourceDiagnostics)
        sourceSettings.update("test") { custom }
        assertNull(source.lastExportMillis)
        val file = ByteArrayOutputStream().also { source.export(it) }.toByteArray()
        assertEquals(clock.millis(), source.lastExportMillis)

        val targetDiagnostics = diagnostics("diag-b")
        val (target, targetSettings) = repository(targetDiagnostics)
        val read = target.read(ByteArrayInputStream(file)) as BackupReadResult.Ok
        target.restore(read.backup)

        assertEquals(custom, targetSettings.settings.first())
        assertFalse(targetDiagnostics.isLoggingEnabled)
        assertEquals(LogLevel.WARN, targetDiagnostics.minLevel)
        assertTrue("custom names never logged (F17.8)", logged.none { it.contains("Time Sink") })
    }

    @Test
    fun f13_7_exportRecordsTheVersionAndImportComparesIt() = runTest {
        val (source, _) = repository(diagnostics("diag-d"))
        val file = ByteArrayOutputStream().also { source.export(it) }.toByteArray()
        val backup = (source.read(ByteArrayInputStream(file)) as BackupReadResult.Ok).backup
        assertEquals("0.0.30", backup.appVersionName)
        assertEquals(BackupAge.SAME, source.ageOf(backup))
        assertEquals(BackupAge.OLDER, source.ageOf(backup.copy(appVersionCode = 29)))
        assertEquals(BackupAge.NEWER, source.ageOf(backup.copy(appVersionCode = 31)))
        assertEquals(BackupAge.UNKNOWN, source.ageOf(backup.copy(appVersionCode = null)))
    }

    @Test
    fun d059_restoreKeepsValuesInTheirAllowedRanges() = runTest {
        val (target, targetSettings) = repository(diagnostics("diag-c"))
        target.restore(SettingsBackup(settings = custom.copy(home = HomeSettings(maxFavorites = 99))))
        assertEquals(10, targetSettings.settings.first().home.maxFavorites)
        assertTrue(logged.contains("backup had values outside the allowed ranges; corrected"))
    }

    @Test
    fun f13_5_olderBackupGetsDefaultsForMissingFields() {
        val old = """{ "format": "zeroos-settings", "schemaVersion": 1, "settings": { "favorites": ["x/x.Main#0"] } }"""
        val backup = (SettingsBackupCodec.decode(old) as BackupReadResult.Ok).backup
        assertEquals(listOf("x/x.Main#0"), backup.settings.favorites)
        assertEquals(LauncherSettings().home, backup.settings.home)
        assertTrue(backup.diagnostics.loggingEnabled)
    }

    @Test
    fun f13_5_newerFileOfTheSameSchemaKeepsKnownSettingsAndDropsTheRest() {
        // A later ZeroOS added a setting, a field and a style this version doesn't know, and wrote a null.
        val newer = """{ "format": "zeroos-settings", "schemaVersion": 1, "futureTopLevel": 1, "settings": {
            "favorites": ["x/x.Main#0"], "futureFeature": { "on": true },
            "screenTime": { "showOnHome": false, "style": "SPARKLINE", "futureField": 3 },
            "home": { "alignment": null, "maxFavorites": 4 } } }"""
        val settings = (SettingsBackupCodec.decode(newer) as BackupReadResult.Ok).backup.settings
        assertEquals(listOf("x/x.Main#0"), settings.favorites)
        assertEquals(false, settings.screenTime.showOnHome)
        assertEquals(LauncherSettings().screenTime.style, settings.screenTime.style)
        assertEquals(LauncherSettings().home.alignment, settings.home.alignment)
        assertEquals(4, settings.home.maxFavorites)
    }

    @Test
    fun f13_5_newerBackupIsRefused() {
        assertEquals(BackupReadResult.TooNew(2), SettingsBackupCodec.decode("""{ "format": "zeroos-settings", "schemaVersion": 2 }"""))
    }

    @Test
    fun f13_2_otherFilesAreNotBackups() {
        assertEquals(BackupReadResult.NotABackup, SettingsBackupCodec.decode("""{ "format": "something-else" }"""))
        assertEquals(BackupReadResult.NotABackup, SettingsBackupCodec.decode("hello"))
        assertEquals(
            BackupReadResult.NotABackup,
            SettingsBackupCodec.decode("""{ "format": "zeroos-settings", "settings": { "home": { "maxFavorites": "lots" } } }""")
        )
    }

    @Test
    fun f13_1_fileNameUsesTodaysDate() {
        assertEquals("zeroos-settings-2026-10-15.json", backupFileName(clock))
    }
}
