package com.zeroos.feature.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.insights.InsightsRepository
import com.zeroos.core.data.insights.UsageSummary
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ScreenTimeSettings
import com.zeroos.core.model.settings.StatsStyle
import com.zeroos.core.resources.R
import com.zeroos.core.ui.DotIcons
import com.zeroos.core.ui.DotLine
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.TextStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What home's stats slot ④ shows (wireframe A3): nothing, the "Tap to show screen time" line, or the stats. */
data class HomeStatsState(
    val settings: ScreenTimeSettings = ScreenTimeSettings(),
    val hasAccess: Boolean = false,
    val summary: UsageSummary? = null
) {
    val showHint: Boolean get() = settings.showOnHome && !settings.hintDismissed && !hasAccess
    val showStats: Boolean get() = settings.showOnHome && !settings.hintDismissed && hasAccess && summary != null
}

/** Home stats (F10.3, F10.9, F10.11): read when home becomes visible, never on a timer. */
@HiltViewModel
class HomeStatsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val insights: InsightsRepository,
    private val permissions: SpecialPermissions
) : ViewModel() {
    private val access = MutableStateFlow(false)
    private val summary = MutableStateFlow<UsageSummary?>(null)

    val state: StateFlow<HomeStatsState> = combine(settings.settings, access, summary) { s, a, sum -> HomeStatsState(s.screenTime, a, sum) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeStatsState())

    /** Home is visible again (or the screen came on): check access and read today's numbers (F10.9). */
    fun refresh() {
        val granted = permissions.isGranted(SpecialPermission.USAGE_ACCESS)
        access.value = granted
        if (granted) viewModelScope.launch { summary.value = insights.summary() }
    }

    /** "×" on "Tap to show screen time": the stats stay hidden until turned on in Settings (F10.11). */
    fun dismissHint() {
        viewModelScope.launch {
            settings.update("screenTime.hintDismissed=true") { it.copy(screenTime = it.screenTime.copy(hintDismissed = true)) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun HomeStatsRoute(onOpenInsights: () -> Unit, onAskAccess: () -> Unit, viewModel: HomeStatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    HomeStats(state, onOpenInsights, onAskAccess, viewModel::dismissHint)
}

/**
 * Slot ④ in the chosen style (F10.5, A3). At the largest text sizes the charts give way to the Text style so the
 * home screen still fits without scrolling (F1.6, A1 layout rules). Tapping opens Insights (F10.10).
 */
@Composable
fun HomeStats(state: HomeStatsState, onOpenInsights: () -> Unit, onAskAccess: () -> Unit, onDismissHint: () -> Unit) {
    val tokens = ZeroTheme.tokens
    if (state.showHint) {
        Row(Modifier.fillMaxWidth().testTag(InsightsTestTags.HINT), verticalAlignment = Alignment.CenterVertically) {
            TextAction(stringResource(R.string.stats_hint), onAskAccess, modifier = Modifier.weight(1f), muted = true)
            val dismiss = stringResource(R.string.common_dismiss)
            TextAction(DISMISS, onDismissHint, modifier = Modifier.semantics { contentDescription = dismiss }, muted = true)
        }
        return
    }
    val summary = state.summary?.takeIf { state.showStats } ?: return
    val roomy = LocalDensity.current.fontScale * tokens.textSizes.body.value <= ROOMY_TEXT_SP
    val style = if (roomy) state.settings.style else StatsStyle.TEXT
    val line = timeAndUnlocks(duration(summary.todayMillis), summary.unlocks)
    val description = stringResource(R.string.stats_description, line)
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onOpenInsights).semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact).testTag(InsightsTestTags.STATS),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.listItemCompact)
    ) {
        when (style) {
            StatsStyle.TEXT -> {
                StatsText(line)
                if (state.settings.compareYesterday) CompareLine(summary)
            }

            StatsStyle.PROGRESS -> ProgressStats(summary, state.settings.goalMinutes ?: DEFAULT_GOAL_MINUTES)

            StatsStyle.WEEKLY -> WeeklyStats(summary, line)

            StatsStyle.HOURLY -> HourlyStats(summary, line)
        }
    }
}

@Composable
internal fun StatsText(text: String, muted: Boolean = false) = Text(
    text,
    color = if (muted) ZeroTheme.tokens.colors.textMuted else ZeroTheme.tokens.colors.text,
    fontSize = ZeroTheme.tokens.textSizes.body
)

/** "↓ 12% vs yesterday" with a dotted arrow (Doto has no ↓/↑, F10.8). Nothing when yesterday had no screen time. */
@Composable
internal fun CompareLine(summary: UsageSummary) {
    val change = changePercent(summary.todayMillis, summary.yesterdaySoFarMillis) ?: return
    val tokens = ZeroTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.listItemCompact)) {
        DotMatrixIcon(
            if (change <= 0) DotIcons.Down else DotIcons.Up,
            height = tokens.spacing.sectionGap * 2,
            contentDescription = stringResource(if (change <= 0) R.string.stats_less else R.string.stats_more),
            color = tokens.colors.textMuted
        )
        StatsText(stringResource(R.string.stats_vs_yesterday, kotlin.math.abs(change)), muted = true)
    }
}

/** "2h 14m / 4h · 38 unlocks" over a dot line, or "4h 23m · over by 23m · 61 unlocks" with it full (F10.7). */
@Composable
private fun ProgressStats(summary: UsageSummary, goalMinutes: Int) {
    val goal = goalMinutes * MILLIS_PER_MINUTE
    val over = summary.todayMillis - goal
    val time = duration(summary.todayMillis)
    val first = if (over > 0) {
        stringResource(R.string.stats_join, time, stringResource(R.string.stats_over, duration(over)))
    } else {
        stringResource(R.string.stats_goal, time, duration(goal))
    }
    StatsText(timeAndUnlocks(first, summary.unlocks))
    DotLine(if (goal > 0) summary.todayMillis.toFloat() / goal else 1f)
}

/** Seven dot columns, today solid and the others hollow, with the totals beside them (A3). */
@Composable
private fun WeeklyStats(summary: UsageSummary, line: String) {
    val tokens = ZeroTheme.tokens
    val locale = LocalConfiguration.current.locales[0]
    val days = summary.lastSevenDays
    val spoken = days.map {
        stringResource(R.string.stats_day_value, it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), duration(it.millis))
    }.joinToString()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.screenGutter)) {
        Column(Modifier.weight(1f)) {
            DotColumns(
                days.map { it.millis },
                rows = CHART_ROWS,
                height = tokens.spacing.statsChartHeight,
                description = stringResource(R.string.stats_week_description, spoken),
                highlight = days.lastIndex,
                hollowOthers = true
            )
            Row(Modifier.fillMaxWidth()) {
                days.forEach {
                    Text(
                        it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        color = tokens.colors.textMuted,
                        fontSize = tokens.textSizes.small,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        StatsText(line)
    }
}

/** 24 thin dot columns for today's hours, an hour axis, then the totals (A3). */
@Composable
private fun HourlyStats(summary: UsageSummary, line: String) {
    val tokens = ZeroTheme.tokens
    val spoken = summary.hourly.withIndex().filter {
        it.value > 0
    }.map { stringResource(R.string.stats_day_value, it.index.toString(), duration(it.value)) }.joinToString()
    DotColumns(
        summary.hourly,
        rows = CHART_ROWS,
        height = tokens.spacing.statsChartHeight,
        description = stringResource(R.string.stats_hours_description, spoken)
    )
    Row(Modifier.fillMaxWidth()) {
        HOUR_MARKS.forEach { Text(it, color = tokens.colors.textMuted, fontSize = tokens.textSizes.small, modifier = Modifier.weight(1f)) }
        Text(stringResource(R.string.stats_now), color = tokens.colors.textMuted, fontSize = tokens.textSizes.small)
    }
    StatsText(line)
}

/** F10.7 / Appendix A: 2 h when no goal is set. */
private const val DEFAULT_GOAL_MINUTES = 120
private const val MILLIS_PER_MINUTE = 60_000L
private const val CHART_ROWS = 4

/** Above this text size (sp, after the system font scale) the charts fall back to text (A1 layout rules). */
private const val ROOMY_TEXT_SP = 20f

/** Hour labels under the timeline (numbers, not words). */
private val HOUR_MARKS = listOf("0", "6", "12", "18")

/** "×" closes the hint (a symbol; its spoken label is a string resource). */
private const val DISMISS = "×"

object InsightsTestTags {
    const val HINT = "stats_hint"
    const val STATS = "home_stats"
    const val SCREEN = "insights"
}
