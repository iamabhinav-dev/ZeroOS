package com.zeroos.feature.insights

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.insights.AppTime
import com.zeroos.core.data.insights.DayTotal
import com.zeroos.core.data.insights.InsightsRepository
import com.zeroos.core.data.insights.UsageSummary
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wellbeing.UsageEvent
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ScreenTimeSettings
import com.zeroos.core.model.settings.StatsStyle
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.testing.MutableClock
import com.zeroos.core.ui.ZeroTheme
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val MIN = 60_000L

/** 2h 14m today, 38 unlocks, yesterday 2h 32m by now (12% less), a shaped week and day. */
internal val sample = UsageSummary(
    todayMillis = 134 * MIN,
    unlocks = 38,
    hourly = List(24) { h -> if (h in 8..20) (h % 5 + 1) * 6 * MIN else 0 },
    topApps = listOf(
        AppTime("yt", 52 * MIN),
        AppTime("wa", 31 * MIN),
        AppTime("ig", 18 * MIN),
        AppTime("ch", 12 * MIN),
        AppTime(
            "maps",
            6 * MIN
        )
    ),
    lastSevenDays = listOf(150, 210, 90, 260, 180, 120, 134).mapIndexed { i, m ->
        DayTotal(
            LocalDate.parse("2026-10-12").plusDays(i.toLong()),
            m * MIN
        )
    },
    yesterdaySoFarMillis = 152 * MIN
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h400dp-xxhdpi")
class HomeStatsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun show(
        settings: ScreenTimeSettings,
        access: Boolean = true,
        theme: ThemeMode = ThemeMode.DARK,
        textSize: TextSize = TextSize.MEDIUM
    ) =
        compose.setContent {
            ZeroTheme(theme, textSize) {
                // Home draws the background behind the slot.
                Box(Modifier.fillMaxSize().background(ZeroTheme.tokens.colors.background)) {
                    HomeStats(HomeStatsState(settings, access, sample), {
                        calls += "insights"
                    }, { calls += "access" }, { calls += "dismiss" })
                }
            }
        }

    @Test
    fun f10_3_f10_10_textStyleAndTapOpensInsights() {
        show(ScreenTimeSettings())
        compose.onNodeWithText("2h 14m · 38 unlocks").assertExists()
        compose.onNodeWithTag(InsightsTestTags.STATS).performClick()
        assertEquals(listOf("insights"), calls)
    }

    @Test
    fun f10_8_comparisonWithYesterday() {
        show(ScreenTimeSettings(compareYesterday = true))
        compose.onNodeWithText("12% vs yesterday").assertExists()
        compose.onNodeWithContentDescription("Less than yesterday").assertExists()
    }

    @Test
    fun f10_7_progressAgainstTheGoalAndOver() {
        show(ScreenTimeSettings(style = StatsStyle.PROGRESS, goalMinutes = 240))
        compose.onNodeWithText("2h 14m / 4h · 38 unlocks").assertExists()
    }

    @Test
    fun f10_7_overTheGoal() {
        show(ScreenTimeSettings(style = StatsStyle.PROGRESS, goalMinutes = 120))
        compose.onNodeWithText("2h 14m · over by 14m · 38 unlocks").assertExists()
    }

    @Test
    fun f10_11_hintUntilAccessThenDismiss() {
        show(ScreenTimeSettings(), access = false)
        compose.onNodeWithText("Tap to show screen time").performClick()
        compose.onNodeWithContentDescription("Dismiss").performClick()
        assertEquals(listOf("access", "dismiss"), calls)
    }

    @Test
    fun f10_3_f10_11_hiddenWhenOffOrDismissed() {
        show(ScreenTimeSettings(hintDismissed = true), access = false)
        compose.onNodeWithTag(InsightsTestTags.HINT).assertDoesNotExist()
        compose.onNodeWithTag(InsightsTestTags.STATS).assertDoesNotExist()
    }

    @Test
    fun f10_13_chartsDescribeTheirValues() {
        show(ScreenTimeSettings(style = StatsStyle.WEEKLY))
        compose.onNodeWithContentDescription("Screen time over the last 7 days", substring = true).assertExists()
    }

    @Test
    fun f10_6_largestTextFallsBackToText() {
        show(ScreenTimeSettings(style = StatsStyle.HOURLY), textSize = TextSize.EXTRA_LARGE)
        compose.onNodeWithText("now").assertDoesNotExist()
        compose.onNodeWithText("2h 14m · 38 unlocks").assertExists()
    }

    @Test
    fun step4_6_textScreenshot() {
        show(ScreenTimeSettings(compareYesterday = true))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_6_progressScreenshot() {
        show(ScreenTimeSettings(style = StatsStyle.PROGRESS, goalMinutes = 240))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_6_weeklyScreenshot() {
        show(ScreenTimeSettings(style = StatsStyle.WEEKLY))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_6_hourlyLightScreenshot() {
        show(ScreenTimeSettings(style = StatsStyle.HOURLY), theme = ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_6_hintScreenshot() {
        show(ScreenTimeSettings(), access = false)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_weeklyScreenshot() {
        show(ScreenTimeSettings(style = StatsStyle.WEEKLY))
        compose.onRoot().captureRoboImage()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class InsightsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val labels = mapOf("yt" to "YouTube", "wa" to "WhatsApp", "ig" to "Instagram", "ch" to "Chrome", "maps" to "Maps")

    @Test
    fun f10_1_f10_2_todayTopAppsAndWeek() {
        var backs = 0
        compose.setContent {
            ZeroTheme { InsightsScreen(InsightsState(sample, labels, compareYesterday = true), {}, onBack = { backs++ }) }
        }
        compose.onNodeWithText("2h 14m").assertExists()
        compose.onNodeWithText("38 unlocks").assertExists()
        compose.onNodeWithText("YouTube").assertExists()
        compose.onNodeWithText("52m").assertExists()
        compose.onNodeWithText("12% vs yesterday").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun f10_11_withoutAccess() {
        var asked = 0
        compose.setContent { ZeroTheme { InsightsScreen(InsightsState(hasAccess = false), { asked++ }, onBack = {}) } }
        compose.onNodeWithText("Tap to show screen time").performClick()
        assertEquals(1, asked)
    }

    @Test
    fun step4_6_insightsScreenshot() {
        compose.setContent { ZeroTheme { InsightsScreen(InsightsState(sample, labels, compareYesterday = true), {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_6_insightsLightScreenshot() {
        compose.setContent { ZeroTheme(ThemeMode.LIGHT) { InsightsScreen(InsightsState(sample, labels), {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_insightsScreenshot() {
        compose.setContent { ZeroTheme { InsightsScreen(InsightsState(sample, labels, compareYesterday = true), {}, onBack = {}) } }
        compose.onRoot().captureRoboImage()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeStatsViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }
    private var granted = false
    private val permissions = mockk<SpecialPermissions>(relaxed = true) {
        every { isGranted(SpecialPermission.USAGE_ACCESS) } answers { granted }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun f10_9_f10_11_refreshReadsOnlyWithAccessAndDismissIsSaved() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        val clock = MutableClock(Instant.parse("2026-10-18T10:00:00Z"), ZoneId.of("+01:00"))
        var reads = 0
        val insights = InsightsRepository(
            { _, _ ->
                reads++
                listOf(UsageEvent("yt", UsageEvent.Type.RESUMED, Instant.parse("2026-10-18T09:00:00Z").toEpochMilli()))
            },
            clock,
            UnconfinedTestDispatcher(testScheduler),
            logger,
            "zeroos",
            true,
            clock::getZone
        )
        val vm = HomeStatsViewModel(settings, insights, permissions)
        backgroundScope.launch { vm.state.collect {} }
        vm.refresh()
        assertTrue(vm.state.first().showHint)
        assertEquals("no access: nothing read", 0, reads)
        granted = true
        vm.refresh()
        assertEquals(60 * MIN, vm.state.first { it.summary != null }.summary!!.todayMillis)
        assertTrue(vm.state.value.showStats)
        vm.dismissHint()
        assertTrue(settings.settings.first { it.screenTime.hintDismissed }.screenTime.hintDismissed)
    }
}
