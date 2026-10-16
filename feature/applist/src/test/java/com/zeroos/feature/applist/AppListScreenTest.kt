package com.zeroos.feature.applist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.apps.numberSameNames
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class AppListScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun app(name: String, profile: ProfileType = ProfileType.MAIN) = AppEntry(
        AppKey("pkg.$name", "Main", profile.ordinal.toLong()),
        name,
        profile
    )
    private val apps = listOf("Calculator", "Calendar", "Camera", "Chrome", "Clock", "Contacts", "Drive", "Gmail", "Instagram", "Zoom").map(
        ::app
    )
    private val calls = mutableListOf<String>()

    private fun show(state: AppListUiState, theme: ThemeMode = ThemeMode.DARK, textSize: TextSize = TextSize.MEDIUM) = compose.setContent {
        ZeroTheme(theme, textSize) {
            AppListScreen(
                state = state,
                onQueryChange = { calls += "query:$it" },
                onAppClick = { calls += it.label },
                onSearchWeb = { calls += "web:$it" },
                onSearchPlayStore = { calls += "play:$it" },
                onClose = { calls += "close" },
                onAppLongClick = { calls += "menu:${it.label}" }
            )
        }
    }

    private fun listState(
        query: String = "",
        results: List<AppEntry> = apps,
        options: AppListSettings = AppListSettings(
            keyboardAutoOpen = false
        )
    ) =
        AppListUiState(query, results, options, loading = false)

    @Test
    fun f2_2_f2_3_showsSearchFieldAndApps() {
        show(listState())
        compose.onNodeWithText("search…").assertExists()
        compose.onNodeWithText("Camera").performClick()
        assertEquals(listOf("Camera"), calls)
    }

    @Test
    fun f2_8_longPressOpensMenu() {
        show(listState())
        compose.onNodeWithText("Gmail").performTouchInput { longClick() }
        assertEquals(listOf("menu:Gmail"), calls)
    }

    @Test
    fun f2_4_typingUpdatesQuery() {
        show(listState())
        compose.onNodeWithTag(AppListTestTags.SEARCH).performTextInput("gm")
        assertEquals(listOf("query:gm"), calls)
    }

    @Test
    fun f2_6_noMatchOffersWebAndPlayStore() {
        show(listState(query = "xyzq", results = emptyList()))
        compose.onNodeWithText("Search the web for \"xyzq\"").performClick()
        compose.onNodeWithText("Search Play Store for \"xyzq\"").performClick()
        assertEquals(listOf("web:xyzq", "play:xyzq"), calls)
    }

    @Test
    fun f2_10_d034_sameNamesAreNumberedAndWorkAppsTagged() {
        val apps = listOf(app("WhatsApp"), app("WhatsApp", ProfileType.CLONE), app("Gmail", ProfileType.WORK)).numberSameNames()
        show(listState(results = apps))
        compose.onNodeWithText("I-WhatsApp").assertExists()
        compose.onNodeWithText("II-WhatsApp").assertExists()
        compose.onNodeWithText("Gmail · work").assertExists()
    }

    @Test
    fun f2_7_fastScrollerJumpsToLetter() {
        show(listState(options = AppListSettings(keyboardAutoOpen = false, fastScroller = true)))
        compose.onNodeWithTag(AppListTestTags.SCROLLER).performTouchInput { click(bottomCenter.copy(y = bottom - 2f)) }
        compose.onNodeWithText("Zoom").assertExists()
    }

    @Test
    fun f2_7_slidingAlongTheScrollerJumpsThroughLetters() {
        show(listState(options = AppListSettings(keyboardAutoOpen = false, fastScroller = true)))
        compose.onNodeWithTag(AppListTestTags.SCROLLER).performTouchInput {
            down(topCenter.copy(y = 2f))
            moveTo(bottomCenter.copy(y = bottom - 2f))
        }
        compose.onNodeWithText("Zoom").assertExists()
        compose.onNodeWithTag(AppListTestTags.SCROLLER).performTouchInput { up() }
    }

    @Test
    fun f2_7_scrollerHighlightsCurrentLetterScreenshot() {
        show(listState(options = AppListSettings(keyboardAutoOpen = false, fastScroller = true)))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f2_7_scrollerWhileSlidingScreenshot() {
        show(listState(options = AppListSettings(keyboardAutoOpen = false, fastScroller = true)))
        // Hold a finger on "G" (Gmail): G stands out, the other letters fade.
        compose.onNodeWithTag(AppListTestTags.SCROLLER).performTouchInput { down(center.copy(y = height * 0.55f)) }
        compose.onRoot().captureRoboImage()
        compose.onNodeWithTag(AppListTestTags.SCROLLER).performTouchInput { up() }
    }

    @Test
    fun f2_11_backAndPullDownClose() {
        show(listState())
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(AppListTestTags.LIST).performTouchInput { swipeDown() }
        assertEquals(listOf("close", "close"), calls)
    }

    @Test
    fun step2_4_listScreenshot() {
        show(listState())
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_4_searchingScreenshot() {
        show(listState(query = "gm", results = listOf(app("Gmail"), app("Google Maps"))))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_4_noMatchScreenshot() {
        show(listState(query = "xyzq", results = emptyList()))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f6_1_listLightScreenshot() {
        show(listState(), theme = ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f6_4_listLargestTextScreenshot() {
        show(listState(), textSize = TextSize.EXTRA_LARGE)
        compose.onRoot().captureRoboImage()
    }
}
