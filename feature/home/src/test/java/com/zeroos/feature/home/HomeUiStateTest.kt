package com.zeroos.feature.home

import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import java.time.Instant
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiStateTest {
    private val time = ZonedDateTime.parse("2026-10-16T10:42:00+05:30[Asia/Kolkata]")
    private fun app(name: String) = AppEntry(AppKey("pkg.$name", "Main", 0), name, true)
    private val apps = listOf("Phone", "Messages", "Camera", "Maps").map(::app)
    private fun key(name: String) = AppKey("pkg.$name", "Main", 0).asString()

    @Test
    fun f1_4_favoritesKeepSavedOrder() {
        val settings = LauncherSettings(favorites = listOf(key("Maps"), key("Phone")))
        assertEquals(listOf("Maps", "Phone"), homeUiState(time, settings, apps, null, null).favorites.map { it.label })
    }

    @Test
    fun f3_4_uninstalledFavoritesAreSkipped() {
        val settings = LauncherSettings(favorites = listOf(key("Gone"), key("Camera")))
        assertEquals(listOf("Camera"), homeUiState(time, settings, apps, null, null).favorites.map { it.label })
    }

    @Test
    fun f3_3_atMostMaxFavoritesAreShown() {
        val settings = LauncherSettings(favorites = apps.map { it.key.asString() }, home = HomeSettings(maxFavorites = 2))
        assertEquals(2, homeUiState(time, settings, apps, null, null).favorites.size)
    }

    @Test
    fun f5_4_customNamesReplaceLabels() {
        val settings = LauncherSettings(favorites = listOf(key("Camera")), renamedApps = mapOf(key("Camera") to "Lens"))
        assertEquals("Lens", homeUiState(time, settings, apps, null, null).favorites.single().label)
    }

    @Test
    fun f1_3_f1_8_batteryAndAlarmFollowSettings() {
        val alarm = Instant.parse("2026-10-17T01:00:00Z")
        val shown = homeUiState(time, LauncherSettings(home = HomeSettings(showBattery = true, showNextAlarm = true)), apps, 82, alarm)
        assertEquals(82, shown.batteryPercent)
        assertEquals("06:30", shown.nextAlarm!!.toLocalTime().toString())

        val hidden = homeUiState(time, LauncherSettings(home = HomeSettings(showBattery = false, showNextAlarm = false)), apps, 82, alarm)
        assertNull(hidden.batteryPercent)
        assertNull(hidden.nextAlarm)
    }
}
