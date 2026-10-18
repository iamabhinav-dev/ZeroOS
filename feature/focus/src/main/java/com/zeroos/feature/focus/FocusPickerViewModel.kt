package com.zeroos.feature.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppSearchIndex
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.FocusIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Focus picker (wireframe H11): [allowed] holds app keys in the order they were picked. */
data class FocusPickerState(
    val icon: FocusIcon = FocusIcon.MIX,
    val minutes: Int? = null,
    val apps: List<AppEntry> = emptyList(),
    val allowed: List<String> = emptyList(),
    /** Filters [apps]; never logged (F17.8). */
    val query: String = "",
    /** The custom length shown on the Custom row; selected when [minutes] equals it and isn't a preset. */
    val custom: Int = CUSTOM_DEFAULT
)

/**
 * Duration and allowed apps (F9.2). The last choice is the default next time (H11); choices survive process death
 * (NFR-R7). Starting saves them and starts the session (F9.1).
 */
@HiltViewModel
class FocusPickerViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val settings: SettingsRepository,
    private val focus: FocusRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    init {
        viewModelScope.launch {
            if (!savedState.contains(KEY_MINUTES)) {
                val last = settings.settings.first().focus
                savedState[KEY_ALLOWED] = ArrayList(last.lastAllowed)
                savedState[KEY_MINUTES] = last.lastMinutes
                if (last.lastMinutes !in FOCUS_PRESETS) savedState[KEY_CUSTOM] = last.lastMinutes
            }
        }
    }

    private val choice = combine(
        savedState.getStateFlow<Int?>(KEY_MINUTES, null),
        savedState.getStateFlow(KEY_CUSTOM, CUSTOM_DEFAULT),
        savedState.getStateFlow(KEY_ALLOWED, ArrayList<String>()),
        savedState.getStateFlow(KEY_QUERY, "")
    ) { minutes, custom, allowed, query -> Choice(minutes, custom, allowed, query) }

    val state: StateFlow<FocusPickerState> = combine(appsRepository.apps, settings.settings, choice) { apps, current, c ->
        val visible = visibleApps(apps, current)
        val keys = visible.map { it.key.asString() }.toSet()
        FocusPickerState(
            current.focus.icon,
            c.minutes,
            AppSearchIndex(visible).search(c.query),
            c.allowed.filter {
                it in keys
            },
            c.query,
            c.custom
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FocusPickerState())

    /** 25 / 50 / 90 min (F9.2). */
    fun selectMinutes(minutes: Int) {
        savedState[KEY_MINUTES] = minutes
    }

    /** Tapping the Custom row picks the custom length shown on it (D-053). */
    fun selectCustom() {
        savedState[KEY_MINUTES] = state.value.custom
    }

    /** "Custom" ‹ ›: picks Custom and goes one step shorter or longer within 5–240 min, stopping at the ends (F9.2, D-051). */
    fun stepCustom(longer: Boolean) {
        val now = state.value.custom
        val next = if (longer) {
            CUSTOM_STEPS.firstOrNull { it > now } ?: CUSTOM_STEPS.last()
        } else {
            CUSTOM_STEPS.lastOrNull { it < now } ?: CUSTOM_STEPS.first()
        }
        savedState[KEY_CUSTOM] = next
        savedState[KEY_MINUTES] = next
    }

    fun onQueryChange(query: String) {
        savedState[KEY_QUERY] = query
    }

    fun toggle(app: AppEntry) {
        val key = app.key.asString()
        val allowed = state.value.allowed
        savedState[KEY_ALLOWED] = ArrayList(if (key in allowed) allowed - key else allowed + key)
    }

    /** Remembers the choice for next time, then starts focus (F9.1, F9.2). */
    fun start(onStarted: (Int) -> Unit) {
        val minutes = state.value.minutes ?: return
        val allowed = state.value.allowed
        viewModelScope.launch {
            settings.update("focus.last=$minutes min, ${allowed.size} apps") {
                it.copy(focus = it.focus.copy(lastMinutes = minutes, lastAllowed = allowed))
            }
            focus.start(minutes, allowed.toSet(), turnOnDnd = settings.settings.first().focus.doNotDisturb)
            onStarted(minutes)
        }
    }

    private companion object {
        const val KEY_MINUTES = "focus_minutes"
        const val KEY_ALLOWED = "focus_allowed"
        const val KEY_QUERY = "focus_query"
        const val KEY_CUSTOM = "focus_custom"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** F9.2 presets. */
val FOCUS_PRESETS = listOf(25, 50, 90)

private data class Choice(val minutes: Int?, val custom: Int, val allowed: List<String>, val query: String)

/** Shown on "Custom" before a custom length is chosen (H11 shows ‹45 min›). */
const val CUSTOM_DEFAULT = 45

/** F9.2 custom: 5–240 min, every 5 min up to an hour, then every 15. */
internal val CUSTOM_STEPS = (5..60 step 5).toList() + (75..240 step 15).toList()
