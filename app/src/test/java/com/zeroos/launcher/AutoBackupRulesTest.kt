package com.zeroos.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.data.settings.SETTINGS_FILE_NAME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/** F13.3: Android Auto Backup and device transfer carry the settings files, and nothing else (NFR-S5). */
@RunWith(AndroidJUnit4::class)
class AutoBackupRulesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsFiles = setOf("file" to "datastore/$SETTINGS_FILE_NAME", "sharedpref" to "$DIAGNOSTICS_PREFS.xml")

    /** (section, domain, path) of every <include>. */
    private fun includes(xml: Int): List<Triple<String, String, String>> {
        val parser = context.resources.getXml(xml)
        val found = mutableListOf<Triple<String, String, String>>()
        var section = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "include" -> found += Triple(section, parser.getAttributeValue(null, "domain"), parser.getAttributeValue(null, "path"))
                else -> section = parser.name
            }
        }
        return found
    }

    @Test
    fun f13_3_backupIsAllowed() {
        assertTrue(context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
    }

    @Test
    fun f13_3_android11AndOlderBackUpOnlySettings() {
        assertEquals(settingsFiles, includes(R.xml.backup_rules).map { it.second to it.third }.toSet())
    }

    @Test
    fun f13_3_android12CloudBackupAndTransferCarryOnlySettings() {
        val rules = includes(R.xml.data_extraction_rules)
        listOf("cloud-backup", "device-transfer").forEach { section ->
            assertEquals(section, settingsFiles, rules.filter { it.first == section }.map { it.second to it.third }.toSet())
        }
    }
}
