package com.zeroos.feature.applist

import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {
    private fun apps(vararg names: String) = names.map { AppEntry(AppKey("pkg.$it", "Main", 0), it) }
    private val index = AppSearchIndex(apps("Calculator", "Calendar", "Camera", "Gmail", "Google Maps", "Ébène", "Maps", "YouTube Music"))
    private fun search(q: String) = index.search(q).map { it.label }

    @Test
    fun f2_4_emptyQueryShowsEverything() = assertEquals(8, search("  ").size)

    @Test
    fun f2_4_caseInsensitiveSubstring() = assertEquals(listOf("Calculator", "Calendar"), search("CAL"))

    @Test
    fun f2_4_initialsMatch() = assertEquals(listOf("Gmail", "Google Maps"), search("gm"))

    @Test
    fun f2_4_wordStartMatchesRankAboveSubstring() = assertEquals(listOf("Maps", "Google Maps"), search("maps"))

    @Test
    fun f2_4_accentInsensitive() = assertEquals(listOf("Ébène"), search("ebe"))

    @Test
    fun f2_4_noMatchIsEmpty() = assertTrue(search("xyzq").isEmpty())

    @Test
    fun f2_4_matchesAnywhereInName() = assertEquals(listOf("YouTube Music"), search("tub"))

    /** NFR-P3: 300 apps, every keystroke well under one 60 Hz frame (16 ms). */
    @Test
    fun nfrP3_searchOver300AppsIsFast() {
        val many = AppSearchIndex((1..300).map { AppEntry(AppKey("p$it", "Main", 0), "Application number $it") })
        repeat(WARMUP) { many.search("app n") }
        val runs = 200
        val start = System.nanoTime()
        repeat(runs) { many.search(if (it % 2 == 0) "an" else "number 2") }
        val perQueryMs = (System.nanoTime() - start) / runs / 1_000_000.0
        assertTrue("search took $perQueryMs ms per query", perQueryMs < 16.0)
    }

    private companion object {
        const val WARMUP = 50
    }
}
