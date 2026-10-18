package com.zeroos.feature.home

import com.zeroos.core.data.focus.FocusSession
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.model.settings.FocusSettings
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.model.settings.LauncherSettings
import java.time.Instant
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiStateTest {
    private val time = ZonedDateTime.parse("2026-10-16T10:42:00+01:00")
    private fun app(name: String) = AppEntry(AppKey("pkg.$name", "Main", 0), name)
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
    fun f4_2_f4_3_gesturesFollowSettings() {
        val off = homeUiState(time, LauncherSettings(), apps, null, null)
        assertNull("side swipes are off by default (D-026)", off.swipeLeftApp)
        assertNull(off.swipeRightApp)

        val on = homeUiState(
            time,
            LauncherSettings(
                gestures = com.zeroos.core.model.settings.GestureSettings(
                    swipeLeftApp = key("Camera"),
                    swipeRightApp = key("Gone")
                )
            ),
            apps,
            null,
            null
        )
        assertEquals("Camera", on.swipeLeftApp?.label)
        assertNull("uninstalled gesture app does nothing", on.swipeRightApp)
    }

    @Test
    fun f6_2_f6_5_wallpaperAndVisibilityFollowAppearance() {
        val s =
            LauncherSettings(
                appearance = com.zeroos.core.model.settings.AppearanceSettings(
                    wallpaper = com.zeroos.core.model.settings.Wallpaper.SYSTEM,
                    showClock = false
                )
            )
        val state = homeUiState(time, s, apps, null, null)
        assertEquals(true, state.showWallpaper)
        assertEquals(false, state.showClock)
        assertEquals(false, homeUiState(time, LauncherSettings(), apps, null, null).showWallpaper)
    }

    @Test
    fun f1_3_f1_8_batteryAndAlarmFollowSettings() {
        val alarm = Instant.parse("2026-10-17T05:30:00Z")
        val shown =
            homeUiState(
                time,
                LauncherSettings(home = HomeSettings(showBattery = true, showNextAlarm = true)),
                apps,
                BatteryStatus(82, charging = true),
                alarm
            )
        assertEquals(82, shown.batteryPercent)
        assertEquals(true, shown.charging)
        assertEquals("06:30", shown.nextAlarm!!.toLocalTime().toString())

        val hidden =
            homeUiState(
                time,
                LauncherSettings(home = HomeSettings(showBattery = false, showNextAlarm = false)),
                apps,
                BatteryStatus(82, charging = true),
                alarm
            )
        assertNull(hidden.batteryPercent)
        assertEquals("no bolt without battery %", false, hidden.charging)
        assertNull(hidden.nextAlarm)
    }

    @Test
    fun f6_7_secondsFollowAppearance() {
        val on = LauncherSettings(appearance = com.zeroos.core.model.settings.AppearanceSettings(showSeconds = true))
        assertEquals(true, homeUiState(time, on, apps, null, null).showSeconds)
        assertEquals(false, homeUiState(time, LauncherSettings(), apps, null, null).showSeconds)
    }

    @Test
    fun f4_5_doubleTapFollowsTheSetting() {
        val on = LauncherSettings(gestures = com.zeroos.core.model.settings.GestureSettings(doubleTapLock = true))
        assertEquals(true, homeUiState(time, on, apps, null, null).doubleTapToLock)
        assertEquals(false, homeUiState(time, LauncherSettings(), apps, null, null).doubleTapToLock)
    }

    @Test
    fun f9_1_f9_3_f9_4_focusFiltersFavoritesAndShowsMinutesLeft() {
        val settings = LauncherSettings(favorites = apps.map { it.key.asString() })
        val base = homeUiState(time, settings, apps, null, null)
        val idle = base.withFocus(FocusSettings(), null)
        assertEquals(4, idle.favorites.size)
        assertEquals(true, idle.showFocus)
        assertEquals(null, idle.focusMinutesLeft)
        assertEquals(false, base.withFocus(FocusSettings(showOnHome = false), null).showFocus)

        val now = time.toInstant().toEpochMilli()
        val session = FocusSession(now, now + 22 * 60_000L + 1, setOf(key("Phone"), key("Maps")))
        val active = base.withFocus(FocusSettings(showOnHome = false, icon = FocusIcon.RIM), session)
        assertEquals(listOf("Phone", "Maps"), active.favorites.map { it.label })
        assertEquals("time left always shows while focusing", true, active.showFocus)
        assertEquals("rounded up", 23, active.focusMinutesLeft)
        assertEquals(FocusIcon.RIM, active.focusIcon)
        assertEquals(1, base.withFocus(FocusSettings(), session.copy(endsAtMillis = now + 1)).focusMinutesLeft)
    }
}
