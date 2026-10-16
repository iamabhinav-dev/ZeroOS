package com.zeroos.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.ZeroTheme
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w392dp-h872dp-xxhdpi")
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val time = ZonedDateTime.parse("2026-10-15T10:42:00+05:30[Asia/Kolkata]")
    private val favorites = listOf("Phone", "Messages", "Camera", "Maps", "WhatsApp", "Spotify")
        .map { AppEntry(AppKey("pkg.$it", "Main", 0), it) }
    private val calls = mutableListOf<String>()

    private fun show(
        state: HomeUiState = HomeUiState(time, favorites = favorites.take(4), batteryPercent = 82),
        slots: HomeSlots = HomeSlots(),
        textSize: TextSize = TextSize.MEDIUM,
        theme: ThemeMode = ThemeMode.DARK
    ) = compose.setContent {
        ZeroTheme(theme, textSize) {
            HomeScreen(
                state = state,
                onSwipe = { calls += "swipe:$it" },
                onLongPress = { calls += "longPress" },
                onFavoriteClick = { calls += it.label },
                onClockClick = { calls += "clock" },
                onDateClick = { calls += "date" },
                slots = slots,
                onFavoriteLongClick = { calls += "menu:${it.label}" }
            )
        }
    }

    @Test
    fun f1_1_f1_2_f1_3_showsClockDateAndBattery() {
        show()
        compose.onNodeWithText("10:42").assertExists()
        compose.onNodeWithText("Thursday 15 October · 82%").assertExists()
    }

    @Test
    fun f1_1_f1_2_tapClockAndDate() {
        show()
        compose.onNodeWithText("10:42").performClick()
        compose.onNodeWithText("Thursday 15 October · 82%").performClick()
        assertEquals(listOf("clock", "date"), calls)
    }

    @Test
    fun f1_4_favoritesAreShownAndLaunch() {
        show()
        compose.onNodeWithText("Maps").assertIsDisplayed().performClick()
        assertEquals(listOf("Maps"), calls)
    }

    @Test
    fun f1_3_boltShownOnlyWhileCharging() {
        show(HomeUiState(time, batteryPercent = 82, charging = true))
        // The bolt is merged into the tappable date row for screen readers, so look in the unmerged tree.
        compose.onNodeWithTag(HomeTestTags.CHARGING, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("charging").assertExists()
    }

    @Test
    fun f1_3_noBoltWhenNotCharging() {
        show(HomeUiState(time, batteryPercent = 82, charging = false))
        compose.onNodeWithTag(HomeTestTags.CHARGING, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun f1_3_chargingScreenshot() {
        show(HomeUiState(time, favorites = favorites.take(4), batteryPercent = 82, charging = true))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f2_8_longPressFavoriteOpensMenu() {
        show()
        compose.onNodeWithText("Camera").performTouchInput { longClick() }
        assertEquals(listOf("menu:Camera"), calls)
    }

    @Test
    fun f1_8_showsNextAlarmWhenSet() {
        show(HomeUiState(time, nextAlarm = time.withHour(6).withMinute(30)))
        compose.onNodeWithText("alarm 06:30").assertExists()
    }

    @Test
    fun f4_1_swipeUpOpensAppList() {
        show()
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeUp() }
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeDown() }
        assertEquals(listOf("swipe:UP", "swipe:DOWN"), calls)
    }

    @Test
    fun f4_2_f4_3_sideSwipesAreReported() {
        show()
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeLeft() }
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeRight() }
        assertEquals(listOf("swipe:LEFT", "swipe:RIGHT"), calls)
    }

    @Test
    fun f1_9_longPressOnEmptySpaceOpensMenu() {
        show()
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { longClick(center) }
        assertEquals(listOf("longPress"), calls)
    }

    @Test
    fun step2_3_homeScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f1_7_centeredScreenshot() {
        show(HomeUiState(time, favorites = favorites.take(4), batteryPercent = 82, alignment = Alignment.CENTER))
        compose.onRoot().captureRoboImage()
    }

    /**
     * Wireframe A5: largest text with every slot filled on a short screen still shows all favorites and the
     * clock; the to-do slot is dropped first.
     */
    @Test
    @Config(qualifiers = "en-rGB-w392dp-h640dp-xxhdpi", fontScale = 1.3f)
    fun f1_6_largestTextKeepsFavoritesVisible() {
        val line: (String) -> (@androidx.compose.runtime.Composable () -> Unit) = { text -> { BodyText(text) } }
        show(
            state = HomeUiState(time, favorites = favorites, batteryPercent = 82, nextAlarm = time.withHour(6).withMinute(30)),
            slots = HomeSlots(
                stats = line("2h 14m · 38 unlocks"),
                focus = line("focus"),
                calendar = line("○ Dentist 4:30 pm"),
                todo = {
                    Column {
                        BodyText("☐ buy milk")
                        BodyText("☐ call mom")
                    }
                }
            ),
            textSize = TextSize.EXTRA_LARGE
        )
        favorites.forEach { compose.onNodeWithText(it.label).assertIsDisplayed() }
        compose.onNodeWithText("10:42").assertIsDisplayed()
        compose.onNodeWithText("☐ buy milk").assertIsNotDisplayed()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f6_1_homeLightScreenshot() {
        show(theme = ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "en-rGB-w872dp-h392dp-land-xxhdpi")
    fun nfrR4_landscapePutsFavoritesBesideTheClock() {
        show(HomeUiState(time, favorites = favorites, batteryPercent = 82, nextAlarm = time.withHour(6).withMinute(30)))
        val clock = compose.onNodeWithText("10:42").getBoundsInRoot()
        val list = compose.onNodeWithTag(HomeTestTags.FAVORITES).getBoundsInRoot()
        val middle = compose.onRoot().getBoundsInRoot().right / 2
        assertTrue("favorites on the right half", list.left >= middle - 1.dp)
        assertTrue("clock on the left half", clock.right <= middle + 1.dp)
        favorites.forEach { compose.onNodeWithText(it.label).assertIsDisplayed() }
        compose.onNodeWithText("alarm", substring = true).assertIsDisplayed()
        compose.onRoot().captureRoboImage()
    }
}
