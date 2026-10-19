package com.zeroos.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ScreenTimeSettings
import com.zeroos.core.model.settings.StatsStyle
import com.zeroos.core.resources.R
import com.zeroos.core.ui.PermissionRequest
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.ToggleRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings → Screen time (F10.3, F10.5, F10.7, F10.8). Changes are logged by [SettingsRepository] (F17.7). */
@HiltViewModel
class ScreenTimeSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    /** For the Usage access explanation (F10.11). */
    val permissions: SpecialPermissions
) : ViewModel() {
    val state: StateFlow<ScreenTimeSettings> = settings.settings.map { it.screenTime }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ScreenTimeSettings())

    /** Turning it on also brings back a dismissed "Tap to show screen time" (F10.11). */
    fun setShowOnHome(on: Boolean) = update("showOnHome=$on") { it.copy(showOnHome = on, hintDismissed = it.hintDismissed && !on) }

    fun cycleStyle() = update("style") { it.copy(style = StatsStyle.entries[(it.style.ordinal + 1) % StatsStyle.entries.size]) }

    /** F10.7: off → 2 h (Appendix A) → … → 8 h → 30 min → 1 h → 1 h 30 → off. */
    fun cycleGoal() = update("goal") { it.copy(goalMinutes = GOAL_STEPS.getOrNull(GOAL_STEPS.indexOf(it.goalMinutes) + 1)) }

    fun setCompare(on: Boolean) = update("compareYesterday=$on") { it.copy(compareYesterday = on) }

    /** Result of the Usage access explanation; granted turns the stats on (F10.11). */
    fun onUsageAccessResult(granted: Boolean) {
        permissions.record(SpecialPermission.USAGE_ACCESS, granted)
        if (granted) setShowOnHome(true)
    }

    private fun update(change: String, transform: (ScreenTimeSettings) -> ScreenTimeSettings) {
        viewModelScope.launch { settings.update("screenTime.$change") { it.copy(screenTime = transform(it.screenTime)) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** F10.7: 30 min–8 h in 30 min steps, starting at the 2 h default. */
internal val GOAL_STEPS: List<Int?> = listOf(null) + (120..480 step 30).toList() + listOf(30, 60, 90)

@Composable
fun ScreenTimeSettingsRoute(onOpenInsights: () -> Unit, onBack: () -> Unit, viewModel: ScreenTimeSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var explaining by rememberSaveable { mutableStateOf(false) }
    if (explaining) {
        PermissionRequest(
            permission = SpecialPermission.USAGE_ACCESS,
            isGranted = { viewModel.permissions.isGranted(SpecialPermission.USAGE_ACCESS) },
            openSettings = { viewModel.permissions.openSettings(SpecialPermission.USAGE_ACCESS) },
            openAppInfo = viewModel.permissions::openAppInfo,
            onResult = {
                explaining = false
                viewModel.onUsageAccessResult(it)
            }
        )
        return
    }
    ScreenTimeSettingsScreen(
        state = state,
        actions = ScreenTimeActions(
            onShowOnHome = { on ->
                if (on &&
                    !viewModel.permissions.isGranted(SpecialPermission.USAGE_ACCESS)
                ) {
                    explaining = true
                } else {
                    viewModel.setShowOnHome(on)
                }
            },
            onStyle = viewModel::cycleStyle,
            onGoal = viewModel::cycleGoal,
            onCompare = viewModel::setCompare,
            onInsights = onOpenInsights
        ),
        onBack = onBack
    )
}

data class ScreenTimeActions(
    val onShowOnHome: (Boolean) -> Unit,
    val onStyle: () -> Unit,
    val onGoal: () -> Unit,
    val onCompare: (Boolean) -> Unit,
    val onInsights: () -> Unit
)

/** Wireframe H4. */
@Composable
fun ScreenTimeSettingsScreen(state: ScreenTimeSettings, actions: ScreenTimeActions, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.settings_screen_time), SettingsTestTags.SCREEN_TIME, onBack) {
        item { ToggleRow(stringResource(R.string.screen_time_show), state.showOnHome && !state.hintDismissed, actions.onShowOnHome) }
        item { SettingRow(stringResource(R.string.screen_time_style), stringResource(styleName(state.style)), actions.onStyle) }
        item { SettingRow(stringResource(R.string.screen_time_goal), goalLabel(state.goalMinutes), actions.onGoal) }
        item { ToggleRow(stringResource(R.string.screen_time_compare), state.compareYesterday, actions.onCompare) }
        item { NavRow(R.string.insights_title, actions.onInsights) }
    }
}

private fun styleName(style: StatsStyle) = when (style) {
    StatsStyle.TEXT -> R.string.screen_time_style_text
    StatsStyle.PROGRESS -> R.string.screen_time_style_progress
    StatsStyle.WEEKLY -> R.string.screen_time_style_weekly
    StatsStyle.HOURLY -> R.string.screen_time_style_hourly
}

@Composable
private fun goalLabel(minutes: Int?): String = when {
    minutes == null -> stringResource(R.string.common_off)
    minutes % MINUTES_PER_HOUR == 0 -> stringResource(R.string.duration_hours, minutes / MINUTES_PER_HOUR)
    minutes < MINUTES_PER_HOUR -> stringResource(R.string.duration_minutes, minutes)
    else -> stringResource(R.string.duration_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}

private const val MINUTES_PER_HOUR = 60
