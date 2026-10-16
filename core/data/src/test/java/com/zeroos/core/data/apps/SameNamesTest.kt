package com.zeroos.core.data.apps

import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import com.zeroos.core.model.settings.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SameNamesTest {
    private fun app(name: String, serial: Long = 0, profile: ProfileType = ProfileType.MAIN, pkg: String = name.lowercase()) =
        AppEntry(AppKey("com.$pkg", "Main", serial), name, profile)

    @Test
    fun d034_cloneGetsTwoMainGetsOne() {
        // The clone is listed first here; the main profile's app still gets I.
        val numbered = listOf(app("WhatsApp", serial = 11, profile = ProfileType.CLONE), app("Gmail"), app("WhatsApp")).numberSameNames()
        assertEquals(listOf(2, null, 1), numbered.map { it.copyNumber })
        assertEquals(listOf("WhatsApp", "Gmail", "WhatsApp"), numbered.map { it.label })
    }

    @Test
    fun d034_numbersAreStableAndCaseInsensitive() {
        val apps =
            listOf(
                app("Notes", serial = 12, profile = ProfileType.CLONE),
                app("notes", pkg = "other"),
                app("NOTES", serial = 10, profile = ProfileType.WORK)
            )
        assertEquals(listOf(3, 1, 2), apps.numberSameNames().map { it.copyNumber })
        assertEquals(apps.numberSameNames(), apps.reversed().numberSameNames().reversed())
    }

    @Test
    fun d034_customNameIsShownWithoutNumber() {
        val clone = listOf(app("WhatsApp"), app("WhatsApp", serial = 11, profile = ProfileType.CLONE)).numberSameNames()[1]
        val renamed = clone.renamedTo("Work chat")
        assertEquals("Work chat", renamed.label)
        assertNull(renamed.copyNumber)
        assertEquals(clone, clone.renamedTo(null))
    }

    @Test
    fun d034_hidingTheOriginalKeepsTheCloneNumbered() {
        val apps = listOf(app("WhatsApp"), app("WhatsApp", serial = 11, profile = ProfileType.CLONE)).numberSameNames()
        val visible = visibleApps(apps, LauncherSettings(hiddenApps = setOf(apps[0].key.asString())))
        assertEquals(2, visible.single().copyNumber)
    }
}
