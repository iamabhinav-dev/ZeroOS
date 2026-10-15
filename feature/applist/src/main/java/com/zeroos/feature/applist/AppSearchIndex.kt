package com.zeroos.feature.applist

import com.zeroos.core.model.AppEntry
import java.text.Normalizer

/**
 * Search over the app list (F2.4), built once per app-list change so each keystroke is cheap (NFR-P3).
 * Case- and accent-insensitive. Results are ordered by match strength, then alphabetically:
 * name starts with the query → a word starts with it → initials start with it ("gm" → Google Maps) → contains it.
 */
class AppSearchIndex(apps: List<AppEntry>) {
    private class Indexed(val app: AppEntry, val name: String, val words: List<String>, val initials: String)

    private val indexed = apps.map { app ->
        val name = normalize(app.label)
        val words = name.split(WORD_SEPARATORS).filter { it.isNotEmpty() }
        Indexed(app, name, words, words.joinToString("") { it.take(1) })
    }

    fun search(query: String): List<AppEntry> {
        val q = normalize(query).trim()
        if (q.isEmpty()) return indexed.map { it.app }
        return indexed.mapIndexedNotNull { order, item -> rank(item, q)?.let { Triple(it, order, item.app) } }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .map { it.third }
    }

    private fun rank(item: Indexed, q: String): Int? = when {
        item.name.startsWith(q) -> RANK_NAME_PREFIX
        item.words.any { it.startsWith(q) } -> RANK_WORD_PREFIX
        q.length > 1 && item.initials.startsWith(q) -> RANK_INITIALS
        item.name.contains(q) -> RANK_CONTAINS
        else -> null
    }

    companion object {
        private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
        private val COMBINING_MARKS = Regex("\\p{Mn}+")
        private const val RANK_NAME_PREFIX = 0
        private const val RANK_WORD_PREFIX = 1
        private const val RANK_INITIALS = 2
        private const val RANK_CONTAINS = 3

        /** Lowercase without accents, e.g. "Ébène" → "ebene". */
        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase()
    }
}
