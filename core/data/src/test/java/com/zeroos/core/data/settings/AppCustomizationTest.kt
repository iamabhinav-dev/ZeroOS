package com.zeroos.core.data.settings

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppCustomizationTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val key = AppKey("com.instagram.android", "Main", 0)
    private val id = key.asString()

    @Test
    fun f5_4_renameTrimsAndLimitsLength() {
        assertEquals("Time Sink", LauncherSettings().renaming(key, "  Time Sink ").renamedApps[id])
        assertEquals(MAX_CUSTOM_NAME_LENGTH, LauncherSettings().renaming(key, "x".repeat(60)).renamedApps[id]!!.length)
    }

    @Test
    fun f5_5_blankOrNullResetsToOriginal() {
        val renamed = LauncherSettings().renaming(key, "Time Sink")
        assertFalse(id in renamed.renaming(key, "   ").renamedApps)
        assertFalse(id in renamed.renaming(key, null).renamedApps)
    }

    @Test
    fun f5_1_hidingAlsoRemovesFromFavoritesAndUnhideRestoresVisibility() {
        val hidden = LauncherSettings(favorites = listOf(id)).hiding(key)
        assertTrue(id in hidden.hiddenApps)
        assertTrue(hidden.favorites.isEmpty())
        assertFalse(id in hidden.unhiding(key).hiddenApps)
    }

    @Test
    fun f17_8_customNamesAreNeverLogged() = runTest {
        val logged = mutableListOf<String>()
        val logger = object : Logger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
                logged += message
            }
        }
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val repo = AppCustomizationRepository(SettingsRepository(store, logger))
        repo.rename(key, "Time Sink")
        repo.hide(key)
        repo.unhide(key)
        repo.rename(key, null)
        assertTrue(store.data.first().renamedApps.isEmpty())
        assertTrue(logged.none { it.contains("Time Sink") })
        assertEquals(
            listOf(
                "rename com.instagram.android=<redacted, 9 chars>",
                "hidden+=com.instagram.android",
                "hidden-=com.instagram.android",
                "rename reset com.instagram.android"
            ),
            logged.map { it.removePrefix("setting changed: ") }
        )
    }
}
