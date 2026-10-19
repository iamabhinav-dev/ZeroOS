package com.zeroos.feature.insights

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.insights.InsightsRepository
import com.zeroos.core.data.insights.UsageSummary
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wellbeing.ShortFormDay
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.PermissionRequest
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

/** Insights (H12): [labels] maps package names to the names shown in ZeroOS (renames applied). */
data class InsightsState(
    val summary: UsageSummary? = null,
    val labels: Map<String, String> = emptyMap(),
    val compareYesterday: Boolean = false,
    val hasAccess: Boolean = true,
    /** Today's short-video time and blocks, shown while the blocker is on (F16.14). */
    val shortForm: ShortFormDay? = null
)

/** Insights (F10.1, F10.2, F10.8): read when the screen opens or comes back, never on a timer (F10.9). */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    apps: AppsRepository,
    settings: SettingsRepository,
    private val insights: InsightsRepository,
    /** Also used by [UsageAccessRoute] for the explanation screen (F10.11). */
    val permissions: SpecialPermissions,
    private val wellbeingState: WellbeingStateRepository
) : ViewModel() {
    private val summary = MutableStateFlow<UsageSummary?>(null)
    private val access = MutableStateFlow(true)
    private val shortForm = MutableStateFlow(ShortFormDay())

    val state: StateFlow<InsightsState> = combine(apps.apps, settings.settings, summary, access, shortForm) { list, s, sum, a, sf ->
        val labels = visibleApps(list, s).associate { it.key.packageName to it.label }
        InsightsState(sum, labels, s.screenTime.compareYesterday, a, sf.takeIf { s.shortForm.enabled })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), InsightsState())

    fun refresh() {
        val granted = permissions.isGranted(SpecialPermission.USAGE_ACCESS)
        access.value = granted
        viewModelScope.launch { shortForm.value = wellbeingState.shortFormToday() }
        if (granted) viewModelScope.launch { summary.value = insights.summary() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * The Usage access explanation, opened from "Tap to show screen time" on home or Insights (F10.11). [onDone] goes
 * back to where it was opened; the stats show there once access is granted.
 */
@Composable
fun UsageAccessRoute(onDone: () -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val permissions = viewModel.permissions
    PermissionRequest(
        permission = SpecialPermission.USAGE_ACCESS,
        isGranted = { permissions.isGranted(SpecialPermission.USAGE_ACCESS) },
        openSettings = { permissions.openSettings(SpecialPermission.USAGE_ACCESS) },
        openAppInfo = permissions::openAppInfo,
        onResult = { granted ->
            permissions.record(SpecialPermission.USAGE_ACCESS, granted)
            onDone()
        }
    )
}

@Composable
fun InsightsRoute(onAskAccess: () -> Unit, onBack: () -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    InsightsScreen(state, onAskAccess, onBack)
}

/** Wireframe H12: today, top apps with dot bars, the last 7 days as dot columns (today solid). */
@Composable
fun InsightsScreen(state: InsightsState, onAskAccess: () -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(InsightsTestTags.SCREEN)) {
        item { BackTitle(stringResource(R.string.insights_title), onBack) }
        if (!state.hasAccess) {
            item { TextAction(stringResource(R.string.stats_hint), onAskAccess, muted = true) }
            return@LazyColumn
        }
        val summary = state.summary ?: return@LazyColumn
        item { BodyText(stringResource(R.string.insights_today), muted = true) }
        item {
            Text(
                duration(summary.todayMillis),
                color = tokens.colors.text,
                fontSize = tokens.textSizes.clock,
                modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter)
            )
        }
        item { UnlocksLine(summary, state.compareYesterday) }
        item { BodyText(stringResource(R.string.insights_top_apps), muted = true) }
        val longest = summary.topApps.firstOrNull()?.millis ?: 1
        items(summary.topApps, key = { it.packageName }) { app ->
            TopAppRow(state.labels[app.packageName] ?: app.packageName, app.millis.toFloat() / longest, duration(app.millis))
        }
        item { BodyText(stringResource(R.string.insights_last_7_days), muted = true) }
        item { WeekChart(summary) }
        state.shortForm?.let { sf ->
            item { BodyText(stringResource(R.string.shorts_title), muted = true) }
            item { BodyText(pluralStringResource(R.plurals.insights_shorts_line, sf.blocks, duration(sf.millis), sf.blocks)) }
        }
    }
}

@Composable
private fun UnlocksLine(summary: UsageSummary, compare: Boolean) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.padding(horizontal = tokens.spacing.screenGutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
    ) {
        summary.unlocks?.let { StatsText(pluralStringResource(R.plurals.stats_unlocks, it, it)) }
        if (compare) CompareLine(summary)
    }
}

/** "YouTube  ●●●●●●●●  52m": longest bar = full width (H12). */
@Composable
private fun TopAppRow(label: String, fraction: Float, time: String) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
    ) {
        Text(label, color = tokens.colors.text, fontSize = tokens.textSizes.body, maxLines = 1, modifier = Modifier.weight(LABEL_SHARE))
        DotBar(fraction, Modifier.weight(BAR_SHARE))
        Text(
            time,
            color = tokens.colors.textMuted,
            fontSize = tokens.textSizes.body,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(TIME_SHARE)
        )
    }
}

@Composable
private fun WeekChart(summary: UsageSummary) {
    val tokens = ZeroTheme.tokens
    val locale = LocalConfiguration.current.locales[0]
    val days = summary.lastSevenDays
    val spoken = days.map {
        stringResource(R.string.stats_day_value, it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), duration(it.millis))
    }.joinToString()
    Column(Modifier.padding(horizontal = tokens.spacing.screenGutter)) {
        DotColumns(
            days.map { it.millis },
            rows = WEEK_ROWS,
            height = tokens.spacing.insightsChartHeight,
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
}

private const val WEEK_ROWS = 8
private const val LABEL_SHARE = 0.35f
private const val BAR_SHARE = 0.45f
private const val TIME_SHARE = 0.2f
