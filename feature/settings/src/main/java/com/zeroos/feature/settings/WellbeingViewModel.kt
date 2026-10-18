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
    /** Usage access granted (needed for daily limits, F8.3, and take a break, F8.16). */
    val usageAccess: Boolean = false,
    /** Accessibility on (needed outside ZeroOS, F8.7, and for take a break, F8.16). */
    val accessibility: Boolean = false
)

/** On/off rows of the Wellbeing page (wireframe H3). */
enum class WellbeingToggle(val logName: String, val apply: (WellbeingSettings, Boolean) -> WellbeingSettings) {
    ENABLED("enabled", { s, on -> s.copy(enabled = on) }),
    PAUSE("pause", { s, on -> s.copy(pauseEnabled = on) }),
    INTENTION("intentionPrompt", { s, on -> s.copy(intentionPrompt = on) }),
    LIMITS("limits", { s, on -> s.copy(limitsEnabled = on) }),
    OUTSIDE("outsideEnforcement", { s, on -> s.copy(outsideEnforcement = on) }),
    BREAKS("breaks", { s, on -> s.copy(breaksEnabled = on) })
}

/** Per-app options in the H5a sheet. */
enum class AppFlag(val logName: String, val apply: (AppRule, Boolean) -> AppRule) {
    PAUSE("pause", { r, on -> r.copy(pause = on) }),
    BREAK("takeBreak", { r, on -> r.copy(takeBreak = on) })
}

/** Rows that step through their options (Appendix A ranges). */
enum class WellbeingCycle(val logName: String, val next: (WellbeingSettings) -> WellbeingSettings) {
    PAUSE_SECONDS("pauseSeconds", { it.copy(pauseSeconds = if (it.pauseSeconds >= MAX_PAUSE) MIN_PAUSE else it.pauseSeconds + 1) }),
    EXTRA_MINUTES("extraMinutesPerDay", { it.copy(extraMinutesPerDay = (it.extraMinutesPerDay + 1) % (MAX_EXTRA + 1)) }),
    BREAK_AFTER("breakAfterMinutes", { it.copy(breakAfterMinutes = BREAK_AFTER_STEPS.nextAfter(it.breakAfterMinutes)) }),
    BREAK_WAIT("breakWaitSeconds", { it.copy(breakWaitSeconds = BREAK_WAIT_STEPS.nextAfter(it.breakWaitSeconds)) }),
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

    private val grantedFlow = MutableStateFlow(grantedNow())

    val state: StateFlow<WellbeingUiState> = combine(
        appsRepository.apps,
        settings.settings,
        savedState.getStateFlow(KEY_QUERY, ""),
        grantedFlow
    ) { apps, current, query, (usage, accessibility) ->
        val rules = current.wellbeing.apps
        // One row per app package (rules are per package); the main profile's entry is shown.
        val byPackage = visibleApps(apps, current).sortedBy { it.profile != ProfileType.MAIN }.distinctBy { it.key.packageName }
        val rows = AppSearchIndex(byPackage).search(query).map { WellbeingAppRow(it, rules[it.key.packageName] ?: AppRule()) }
        WellbeingUiState(current.wellbeing, rows.sortedBy { it.rule.isEmpty }, query, usage, accessibility)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WellbeingUiState())

    fun setToggle(toggle: WellbeingToggle, on: Boolean) = update("${toggle.logName}=$on") { toggle.apply(it, on) }

    fun cycle(option: WellbeingCycle) = update(option.logName, option.next)

    /** F8.1 or F8.16 for one app. */
    fun setAppFlag(packageName: String, flag: AppFlag, on: Boolean) = updateRule(packageName, "${flag.logName}=$on") { flag.apply(it, on) }

    /** F8.3: off → 30 → … → 240 → 5 → … → 20 → off (see [LIMIT_STEPS]). */
    fun cycleAppLimit(packageName: String) = updateRule(packageName, "limit") { rule ->
        rule.copy(dailyLimitMinutes = LIMIT_STEPS.getOrNull(LIMIT_STEPS.indexOf(rule.dailyLimitMinutes) + 1))
    }

    fun onQueryChange(query: String) {
        savedState[KEY_QUERY] = query
    }

    /** Checked again whenever the page comes back to the front. */
    fun refreshPermissions() {
        grantedFlow.value = grantedNow()
    }

    /**
     * Result of a permission explanation, asked when a part that needs it is switched on (F8.11). Declining Usage
     * access while turning Wellbeing on still turns it on, with the mindful pause only.
     */
    fun onPermissionResult(permission: SpecialPermission, granted: Boolean, turningOn: WellbeingToggle) {
        permissions.record(permission, granted)
        refreshPermissions()
        when {
            permission == SpecialPermission.USAGE_ACCESS && turningOn == WellbeingToggle.ENABLED -> update("enabled=true limits=$granted") {
                it.copy(
                    enabled = true,
                    limitsEnabled =
                        granted && it.limitsEnabled
                )
            }

            // Take a break needs both: after Usage access the page asks for Accessibility (F8.11).
            granted && neededPermission(turningOn, state.value.settings, grantedFlow.value.first, grantedFlow.value.second) == null ->
                setToggle(turningOn, true)
        }
    }

    private fun grantedNow() =
        permissions.isGranted(SpecialPermission.USAGE_ACCESS) to permissions.isGranted(SpecialPermission.ACCESSIBILITY)

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

/** The permission [toggle] still needs before it can turn on, or null (F8.11). */
internal fun neededPermission(
    toggle: WellbeingToggle,
    settings: WellbeingSettings,
    usage: Boolean,
    accessibility: Boolean
): SpecialPermission? {
    val wantsUsage = toggle == WellbeingToggle.LIMITS || toggle == WellbeingToggle.BREAKS ||
        (toggle == WellbeingToggle.ENABLED && settings.limitsEnabled)
    val wantsAccessibility = toggle == WellbeingToggle.OUTSIDE || toggle == WellbeingToggle.BREAKS
    return when {
        wantsUsage && !usage -> SpecialPermission.USAGE_ACCESS
        wantsAccessibility && !accessibility -> SpecialPermission.ACCESSIBILITY
        else -> null
    }
}

/** F8.18: take a break after 5–60 min, wait 3–30 s after tapping (Appendix A). */
private val BREAK_AFTER_STEPS = listOf(5, 10, 15, 20, 30, 45, 60)
private val BREAK_WAIT_STEPS = listOf(3, 5, 10, 15, 20, 30)

/** The step after [current], back to the first after the last (or for a value not in the list). */
private fun List<Int>.nextAfter(current: Int) = getOrElse(indexOf(current) + 1) { first() }

/** F8.4: 0–5 uses a day. */
private const val MAX_EXTRA = 5

/** F8.3: 5–240 min; the first step after "off" is the 30 min default (Appendix A), then up, then the short ones. */
internal val LIMIT_STEPS = listOf(null, 30, 45, 60, 90, 120, 180, 240, 5, 10, 15, 20)
