package com.zeroos.core.data.settings

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FavoritesTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun key(pkg: String) = AppKey(pkg, "$pkg.Main", 0)
    private val a = key("a")
    private val b = key("b")
    private val c = key("c")
    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    @Test
    fun f3_1_addsAtTheEndOnce() {
        val (once, r1) = LauncherSettings().addingFavorite(a)
        val (twice, r2) = once.addingFavorite(a)
        assertEquals(AddFavoriteResult.ADDED, r1)
        assertEquals(AddFavoriteResult.ALREADY_FAVORITE, r2)
        assertEquals(listOf(a.asString()), twice.favorites)
    }

    @Test
    fun f3_3_refusesWhenFull() {
        val full = LauncherSettings(favorites = listOf(a.asString(), b.asString()), home = HomeSettings(maxFavorites = 2))
        assertEquals(AddFavoriteResult.FULL, full.addingFavorite(c).second)
    }

    @Test
    fun f3_1_removes() = assertEquals(
        listOf(b.asString()),
        LauncherSettings(favorites = listOf(a.asString(), b.asString())).removingFavorite(a).favorites
    )

    @Test
    fun f3_2_movesUpAndDownIgnoringBadPositions() {
        val s = LauncherSettings(favorites = listOf(a, b, c).map { it.asString() })
        assertEquals(listOf(c, a, b).map { it.asString() }, s.movingFavorite(2, 0).favorites)
        assertEquals(listOf(b, a, c).map { it.asString() }, s.movingFavorite(0, 1).favorites)
        assertEquals(s, s.movingFavorite(0, 5))
    }

    @Test
    fun f3_4_dropsUninstalledFavorites() {
        val s = LauncherSettings(favorites = listOf(a, b, c).map { it.asString() })
        assertEquals(listOf(a, c).map { it.asString() }, s.withoutUninstalledFavorites(setOf("a", "c", "other")).favorites)
    }

    @Test
    fun f3_4_repositoryNeverPrunesWithEmptyAppListAndLogsOnlyRealChanges() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val repo = FavoritesRepository(SettingsRepository(store, logger))
        assertEquals(AddFavoriteResult.ADDED, repo.add(a))
        repo.add(b)
        repo.move(1, 0)
        repo.pruneUninstalled(emptySet())
        repo.pruneUninstalled(setOf("a", "b"))
        assertEquals(listOf(b, a).map { it.asString() }, store.data.first().favorites)
        repo.pruneUninstalled(setOf("b"))
        repo.remove(b)
        assertTrue(store.data.first().favorites.isEmpty())
        assertEquals(
            listOf("favorites+=a", "favorites+=b", "favorites moved 1->0", "favorites pruned (uninstalled apps)", "favorites-=b"),
            logged.map { it.removePrefix("setting changed: ") }
        )
    }
}
