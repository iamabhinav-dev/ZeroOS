package com.zeroos.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppSearchIndex
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.ProfileType
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.DisableFriction
import com.zeroos.core.model.settings.WellbeingSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One app on the Wellbeing → Apps page (wireframe H5). */
data class WellbeingAppRow(val app: AppEntry, val rule: AppRule)

data class WellbeingUiState(
    val settings: WellbeingSettings = WellbeingSettings(),
    /** Apps with a rule first, then all others, each alphabetical (H5); filtered by [query]. */
    val apps: List<WellbeingAppRow> = emptyList(),
    val query: String = "",
    /** Usage access granted (needed for daily limits, F8.3). */
    val usageAccess: Boolean = false
)

/** On/off rows of the Wellbeing page (wireframe H3). */
enum class WellbeingToggle(val logName: String, val apply: (WellbeingSettings, Boolean) -> WellbeingSettings) {
    ENABLED("enabled", { s, on -> s.copy(enabled = on) }),
    PAUSE("pause", { s, on -> s.copy(pauseEnabled = on) }),
    INTENTION("intentionPrompt", { s, on -> s.copy(intentionPrompt = on) }),
    LIMITS("limits", { s, on -> s.copy(limitsEnabled = on) }),
    PUNS("puns", { s, on -> s.copy(puns = on) })
}

/** Rows that step through their options (Appendix A ranges). */
enum class WellbeingCycle(val logName: String, val next: (WellbeingSettings) -> WellbeingSettings) {
    PAUSE_SECONDS("pauseSeconds", { it.copy(pauseSeconds = if (it.pauseSeconds >= MAX_PAUSE) MIN_PAUSE else it.pauseSeconds + 1) }),
    EXTRA_MINUTES("extraMinutesPerDay", { it.copy(extraMinutesPerDay = (it.extraMinutesPerDay + 1) % (MAX_EXTRA + 1)) }),
    FRICTION("disableFriction", {
        it.copy(
            disableFriction = DisableFriction.entries[
                (it.disableFriction.ordinal + 1) %
                    DisableFriction.entries.size
            ]
        )
    })
}

/** Settings → Wellbeing and its Apps page (F8.8–F8.12, wireframes H3, H5). Every change is logged (F17.7). */
@HiltViewModel
class WellbeingViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val settings: SettingsRepository,
    /** Used by the pages for the Usage access explanation (Step 4.1). */
    val permissions: SpecialPermissions,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val usageAccess = MutableStateFlow(permissions.isGranted(SpecialPermission.USAGE_ACCESS))

    val state: StateFlow<WellbeingUiState> = combine(
        appsRepository.apps,
        settings.settings,
        savedState.getStateFlow(KEY_QUERY, ""),
        usageAccess
    ) { apps, current, query, usage ->
        val rules = current.wellbeing.apps
        // One row per app package (rules are per package); the main profile's entry is shown.
        val byPackage = visibleApps(apps, current).sortedBy { it.profile != ProfileType.MAIN }.distinctBy { it.key.packageName }
        val rows = AppSearchIndex(byPackage).search(query).map { WellbeingAppRow(it, rules[it.key.packageName] ?: AppRule()) }
        WellbeingUiState(current.wellbeing, rows.sortedBy { it.rule.isEmpty }, query, usage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WellbeingUiState())

    fun setToggle(toggle: WellbeingToggle, on: Boolean) = update("${toggle.logName}=$on") { toggle.apply(it, on) }

    fun cycle(option: WellbeingCycle) = update(option.logName, option.next)

    /** F8.1 for one app. */
    fun setAppPause(packageName: String, on: Boolean) = updateRule(packageName, "pause=$on") { it.copy(pause = on) }

    /** F8.3: off → 30 → … → 240 → 5 → … → 20 → off (see [LIMIT_STEPS]). */
    fun cycleAppLimit(packageName: String) = updateRule(packageName, "limit") { rule ->
        rule.copy(dailyLimitMinutes = LIMIT_STEPS.getOrNull(LIMIT_STEPS.indexOf(rule.dailyLimitMinutes) + 1))
    }

    fun onQueryChange(query: String) {
        savedState[KEY_QUERY] = query
    }

    /** Checked again whenever the page comes back to the front. */
    fun refreshUsageAccess() {
        usageAccess.value = permissions.isGranted(SpecialPermission.USAGE_ACCESS)
    }

    /**
     * Result of the Usage access explanation, asked when Wellbeing or daily limits are switched on (F8.11).
     * Without it Wellbeing can still run with the mindful pause only.
     */
    fun onUsageAccessResult(granted: Boolean, turningOn: WellbeingToggle) {
        permissions.record(SpecialPermission.USAGE_ACCESS, granted)
        refreshUsageAccess()
        when {
            turningOn == WellbeingToggle.ENABLED -> update("enabled=true limits=$granted") {
                it.copy(
                    enabled = true,
                    limitsEnabled =
                        granted && it.limitsEnabled
                )
            }

            granted -> setToggle(turningOn, true)
        }
    }

    private fun updateRule(packageName: String, change: String, transform: (AppRule) -> AppRule) =
        update("app $packageName $change") { w ->
            val rule = transform(w.apps[packageName] ?: AppRule())
            w.copy(apps = if (rule.isEmpty) w.apps - packageName else w.apps + (packageName to rule))
        }

    private fun update(change: String, transform: (WellbeingSettings) -> WellbeingSettings) {
        viewModelScope.launch { settings.update("wellbeing.$change") { it.copy(wellbeing = transform(it.wellbeing)) } }
    }

    private companion object {
        const val KEY_QUERY = "wellbeing_query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** F8.1: 3–10 s. */
private const val MIN_PAUSE = 3
private const val MAX_PAUSE = 10

/** F8.4: 0–5 uses a day. */
private const val MAX_EXTRA = 5

/** F8.3: 5–240 min; the first step after "off" is the 30 min default (Appendix A), then up, then the short ones. */
internal val LIMIT_STEPS = listOf(null, 30, 45, 60, 90, 120, 180, 240, 5, 10, 15, 20)
