package com.zeroos.core.data.shortform

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormRulesTest {
    private val rules = ShortFormRules.parse(File("../../app/src/main/assets/shortform_rules.json").readText())!!

    /** View IDs from a recorded screen-structure dump (layout lines only, never on-screen text). */
    private fun idsIn(file: File): Set<String> =
        file.readLines().mapNotNull { line -> Regex("""\s([a-z][\w.]*:id/\w+)\s""").find(line)?.groupValues?.get(1) }.toSet()

    private fun dumps(dir: String): List<File> =
        File(javaClass.classLoader!!.getResource("shortform/$dir")!!.toURI()).listFiles()!!.filter { it.extension == "txt" }.sorted()

    private fun packageOf(file: File) = file.readLines().first().removePrefix("package: ").trim()

    @Test
    fun f16_12_bundledRulesParse() {
        assertEquals(1, rules.version)
        assertEquals(listOf("com.google.android.youtube", "com.instagram.android"), rules.apps.map { it.packageName })
    }

    @Test
    fun f16_3_f16_6_shortsAndReelsAreDetected() {
        val shorts = dumps("youtube-shorts-screens") + dumps("instagram-reels-screens")
        assertTrue(shorts.isNotEmpty())
        shorts.forEach { assertTrue(it.name, rules.isShortForm(packageOf(it), idsIn(it))) }
    }

    @Test
    fun f16_3_normalScreensAreNotBlocked() {
        val normal = dumps("youtube-normal-screens") + dumps("instagram-normal-screens")
        assertEquals("30 YouTube + 28 Instagram screens", 58, normal.size)
        normal.forEach { assertFalse(it.name, rules.isShortForm(packageOf(it), idsIn(it))) }
    }

    @Test
    fun f16_12_versioningAndBrokenFiles() {
        assertNull("not JSON", ShortFormRules.parse("{oops"))
        assertNull("newer format", ShortFormRules.parse("""{"version": 2, "apps": []}"""))
        assertNull("missing apps", ShortFormRules.parse("""{"version": 1}"""))
        assertNotNull("unknown fields are ignored", ShortFormRules.parse("""{"version": 1, "apps": [], "comment": "x"}"""))
    }

    @Test
    fun f16_12_minAppVersionAndOtherApps() {
        val r = ShortFormRules(1, listOf(ShortFormApp("a", "A", listOf("feed"), minAppVersion = 100)))
        assertTrue(r.isShortForm("a", setOf("a:id/feed"), appVersion = 100))
        assertFalse("older app version: rules don't apply", r.isShortForm("a", setOf("a:id/feed"), appVersion = 99))
        assertFalse("not a target app", r.isShortForm("b", setOf("a:id/feed")))
    }
}
