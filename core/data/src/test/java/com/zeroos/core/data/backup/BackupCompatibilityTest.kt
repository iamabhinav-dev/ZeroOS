package com.zeroos.core.data.backup

import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import com.zeroos.core.model.settings.WellbeingSettings
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * D-059: a real export from every release is kept in `src/test/resources/backups/`; each must still import with
 * nothing lost. If a change breaks one, raise the schema version and add a migration instead of editing the file.
 */
class BackupCompatibilityTest {
    private val backups: List<File> =
        File(requireNotNull(javaClass.classLoader?.getResource("backups")).toURI()).listFiles { f -> f.extension == "json" }
            .orEmpty()
            .sortedBy { it.name }

    @Test
    fun d059_everyReleaseBackupImportsWithNothingLost() {
        assertTrue("no saved release backups", backups.isNotEmpty())
        for (file in backups) {
            val text = file.readText()
            val read = SettingsBackupCodec.decode(text)
            if (read !is BackupReadResult.Ok) fail("${file.name} no longer imports: $read")
            val again = Json.parseToJsonElement(SettingsBackupCodec.encode((read as BackupReadResult.Ok).backup))
            assertContains(file.name, Json.parseToJsonElement(text), again, path = "")
        }
    }

    @Test
    fun d059_releaseBackupsAreAlreadyWithinTheAllowedRanges() {
        for (file in backups) {
            val settings = (SettingsBackupCodec.decode(file.readText()) as BackupReadResult.Ok).backup.settings
            assertEquals(file.name, settings, settings.withinAllowedRanges())
        }
    }

    @Test
    fun d059_valuesOutsideAppendixARangesAreCorrected() {
        val wild = LauncherSettings(
            favorites = (1..15).map { "p$it/p$it.Main#0" },
            home = HomeSettings(maxFavorites = 99),
            wellbeing = WellbeingSettings(
                pauseSeconds = 0,
                extraMinutesPerDay = 50,
                breakAfterMinutes = 1,
                breakWaitSeconds = 600,
                apps = mapOf("x" to AppRule(dailyLimitMinutes = 1000), "y" to AppRule(dailyLimitMinutes = null))
            )
        )
        val fixed = wild.withinAllowedRanges()
        assertEquals(10, fixed.favorites.size)
        assertEquals(10, fixed.home.maxFavorites)
        assertEquals(3, fixed.wellbeing.pauseSeconds)
        assertEquals(5, fixed.wellbeing.extraMinutesPerDay)
        assertEquals(5, fixed.wellbeing.breakAfterMinutes)
        assertEquals(30, fixed.wellbeing.breakWaitSeconds)
        assertEquals(240, fixed.wellbeing.apps.getValue("x").dailyLimitMinutes)
        assertEquals(null, fixed.wellbeing.apps.getValue("y").dailyLimitMinutes)
        assertEquals("defaults are untouched", LauncherSettings(), LauncherSettings().withinAllowedRanges())
    }

    /** Every key and value of [expected] is still in [actual]; [actual] may have more (settings added since). */
    private fun assertContains(file: String, expected: JsonElement, actual: JsonElement, path: String) {
        when (expected) {
            is JsonObject -> {
                if (actual !is JsonObject) fail("$file: $path is no longer an object")
                for ((key, value) in expected) {
                    val now = (actual as JsonObject)[key]
                    if (now == null) fail("$file: $path.$key was dropped") else assertContains(file, value, now, "$path.$key")
                }
            }

            is JsonArray -> assertEquals("$file: $path changed", expected, actual)

            else -> assertEquals("$file: $path changed", expected, actual)
        }
    }
}
