package com.zeroos.feature.wellbeing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.lock.ScreenTimeLock
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.data.wellbeing.PunList
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.model.AppEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which screen-time lock page is showing (wireframes L3, L4, L5). */
enum class LockPage { LOCKED, UNLOCK, WRONG }

data class LockUiState(
    val page: LockPage = LockPage.LOCKED,
    val usedMillis: Long = 0,
    /** The phrase to type, shown on the unlock page so it can never lock anyone out (D-062). */
    val phrase: String = "",
    val pun: Int = 0,
    val wrongPun: Int = 0
)

/** Sizes of the lock's two pun lists (docs/puns.md F and G). */
data class LockPunCounts(val lock: Int, val wrong: Int)

/**
 * The lock screen inside ZeroOS (F19.3–F19.5): shown for an app [LaunchGate] held back, or for one the lock screen
 * over another app sent here to unlock. The typed phrase stays in the screen's memory only and is never logged.
 */
@HiltViewModel
class LockViewModel @Inject constructor(
    private val gate: LaunchGate,
    private val lock: ScreenTimeLock,
    private val settings: SettingsRepository,
    private val wellbeingState: WellbeingStateRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(LockUiState(page = savedState[KEY_PAGE] ?: LockPage.LOCKED))
    val state: StateFlow<LockUiState> = mutableState.asStateFlow()

    /** The app waiting behind the lock screen. */
    val pending: StateFlow<AppEntry?> = gate.lockFor

    /** ZeroOS came to the front: the lock may have come on or ended meanwhile. */
    fun refresh() = gate.refreshLock()

    /** The lock screen appeared for an app: today's numbers, the phrase and a pun (F19.3). */
    fun show(puns: LockPunCounts) {
        viewModelScope.launch {
            val phrase = settings.settings.first().screenTimeLock.phrase
            val pun = wellbeingState.nextPun(PunList.LOCK, puns.lock)
            mutableState.update { it.copy(usedMillis = lock.state.value.usedMillis, phrase = phrase, pun = pun) }
        }
    }

    fun openUnlock() = setPage(LockPage.UNLOCK)

    /** F19.4, F19.5: the right phrase opens the app for 15 minutes; a wrong one shows a pun and asks again. */
    fun submit(typed: String, puns: LockPunCounts) {
        val app = gate.lockFor.value ?: return
        viewModelScope.launch {
            if (lock.unlock(typed)) {
                setPage(LockPage.LOCKED)
                gate.open(app)
            } else {
                val pun = wellbeingState.nextPun(PunList.LOCK_WRONG, puns.wrong)
                mutableState.update { it.copy(wrongPun = pun) }
                setPage(LockPage.WRONG)
            }
        }
    }

    fun tryAgain() = setPage(LockPage.UNLOCK)

    /** Back from the unlock page returns to the lock screen; from the lock screen it goes home. */
    fun back() = if (state.value.page == LockPage.UNLOCK) setPage(LockPage.LOCKED) else goHome()

    fun goHome() {
        setPage(LockPage.LOCKED)
        gate.dismiss()
    }

    private fun setPage(page: LockPage) {
        savedState[KEY_PAGE] = page
        mutableState.update { it.copy(page = page) }
    }

    private companion object {
        const val KEY_PAGE = "lock_page"
    }
}
