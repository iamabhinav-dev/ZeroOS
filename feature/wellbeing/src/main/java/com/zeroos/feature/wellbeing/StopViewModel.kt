package com.zeroos.feature.wellbeing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.data.wellbeing.PunList
import com.zeroos.core.data.wellbeing.StopScreen
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStatus
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.AppEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The stop screen for [app]: null [screen] while today's usage is still being read. [pun] indexes the line list. */
data class StopUiState(val app: AppEntry? = null, val screen: StopScreen? = null, val pun: Int = 0)

/** Sizes of the two pun lists, read from resources by the screen (F8.13). */
data class PunCounts(val pause: Int, val limit: Int)

/** Mindful pause and limit screens (F8.1–F8.4, F8.13). Logs what happened (F17.7), never the intention (F17.8). */
@HiltViewModel
class StopViewModel @Inject constructor(
    private val gate: LaunchGate,
    private val settings: SettingsRepository,
    private val status: WellbeingStatus,
    private val wellbeingState: WellbeingStateRepository,
    private val logger: Logger
) : ViewModel() {
    private val mutableState = MutableStateFlow(StopUiState())
    val state: StateFlow<StopUiState> = mutableState.asStateFlow()

    /** The app waiting for a stop screen (from [LaunchGate]). */
    val pending: StateFlow<AppEntry?> = gate.stopFor

    /** Decides what [app] needs; opens it at once if it's under its limit and has no pause. */
    fun check(app: AppEntry, puns: PunCounts) {
        if (mutableState.value.app == app) return
        mutableState.value = StopUiState(app)
        viewModelScope.launch {
            val wellbeing = settings.settings.first().wellbeing
            val pkg = app.key.packageName
            val screen = status.stopFor(wellbeing, pkg)
            if (screen == null) {
                finish { gate.open(app) }
                return@launch
            }
            // Puns are always shown (D-045).
            val pun = when (screen) {
                is StopScreen.Pause -> wellbeingState.nextPun(PunList.PAUSE, puns.pause)
                is StopScreen.Limit -> wellbeingState.nextPun(PunList.LIMIT, puns.limit)
            }
            logger.info(TAG, logLine(pkg, screen))
            mutableState.value = StopUiState(app, screen, pun)
        }
    }

    /** "Open …" after the pause (F8.1). */
    fun openAnyway() {
        val app = mutableState.value.app ?: return
        logger.info(TAG, "pause: opened ${app.key.packageName}")
        finish { gate.open(app) }
    }

    /** "5 more minutes" (F8.4). */
    fun moreMinutes() {
        val app = mutableState.value.app ?: return
        viewModelScope.launch {
            wellbeingState.useExtra(app.key.packageName)
            finish { gate.open(app) }
        }
    }

    /** "Go back", "Go home", Back or the Home button: the app stays closed. */
    fun goBack() {
        val app = gate.stopFor.value ?: return
        logger.info(TAG, "stop: went back from ${app.key.packageName}")
        finish { gate.dismiss() }
    }

    private inline fun finish(action: () -> Unit) {
        action()
        mutableState.value = StopUiState()
    }

    private fun logLine(pkg: String, screen: StopScreen) = when (screen) {
        is StopScreen.Pause -> "pause shown for $pkg (${screen.seconds} s)"
        is StopScreen.Limit -> "limit reached for $pkg (${screen.usedMinutes} min used, ${screen.extrasLeft} extra left)"
    }

    private companion object {
        const val TAG = "wellbeing"
    }
}
