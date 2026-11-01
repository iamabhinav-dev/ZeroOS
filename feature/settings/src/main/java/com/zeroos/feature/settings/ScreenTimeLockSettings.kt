package com.zeroos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.lock.ScreenTimeLock
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ScreenTimeLockSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LockSettingsState(
    val settings: ScreenTimeLockSettings = ScreenTimeLockSettings(),
    val usedMillis: Long = 0,
    /** Every app, for the essential-apps picker, filtered by [query]. */
    val apps: List<AppEntry> = emptyList(),
    val query: String = "",
    val accessibilityOn: Boolean = true
)

/** A change that weakens the lock while it's on, so it needs the phrase first (F19.6). */
sealed interface LockChange {
    data object TurnOff : LockChange

    data object RaiseCap : LockChange

    data class AddApp(val key: String) : LockChange

    data class NewPhrase(val phrase: String) : LockChange
}

/** Settings → Wellbeing → Screen-time lock (F19.1, F19.6, wireframes L1, L2). The phrase is never logged. */
@HiltViewModel
class ScreenTimeLockSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val lock: ScreenTimeLock,
    apps: AppsRepository,
    val permissions: SpecialPermissions
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val accessibility = MutableStateFlow(true)

    val state: StateFlow<LockSettingsState> = combine(settings.settings, lock.state, apps.apps, query, accessibility) { s, l, all, q, a ->
        val visible = visibleApps(all, s).filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
        // Essential apps first, then the rest alphabetically, as in the Focus picker (H11).
        val picked = s.screenTimeLock.essentialApps.toSet()
        LockSettingsState(s.screenTimeLock, l.usedMillis, visible.sortedByDescending { it.key.asString() in picked }, q, a)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LockSettingsState())

    /** Coming back to the page: permissions and today's screen time may have changed. */
    fun refresh() {
        accessibility.value = permissions.isGranted(SpecialPermission.ACCESSIBILITY)
        viewModelScope.launch { lock.refresh() }
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    /** Whether [change] needs the phrase first: only while the lock is on (F19.6). */
    fun needsPhrase(): Boolean = state.value.settings.enabled

    /** Applies [change] if [typed] is the phrase (or none is needed); false on a wrong phrase. */
    suspend fun confirm(change: LockChange, typed: String?): Boolean {
        if (needsPhrase() && (typed == null || !lock.isPhrase(typed))) return false
        apply(change)
        return true
    }

    fun turnOn() = update("screenTimeLock.enabled=true") { it.copy(enabled = true) }

    /** ‹: a lower cap only makes the lock stricter, so it never needs the phrase (F19.6). */
    fun lowerCap() = update("screenTimeLock.cap lowered") { it.copy(capMinutes = (it.capMinutes - CAP_STEP).coerceAtLeast(CAP_MIN)) }

    /** Removing an essential app only makes the lock stricter (F19.6). */
    fun removeApp(key: String) = update("screenTimeLock.essentialApps -1") { it.copy(essentialApps = it.essentialApps - key) }

    private fun apply(change: LockChange) = when (change) {
        LockChange.TurnOff -> update("screenTimeLock.enabled=false") { it.copy(enabled = false) }

        LockChange.RaiseCap -> update("screenTimeLock.cap raised") {
            it.copy(capMinutes = (it.capMinutes + CAP_STEP).coerceAtMost(CAP_MAX))
        }

        is LockChange.AddApp -> update("screenTimeLock.essentialApps +1") { it.copy(essentialApps = it.essentialApps + change.key) }

        // Free text: logged only as its length (F17.8).
        is LockChange.NewPhrase -> update("screenTimeLock.phrase=<redacted, ${change.phrase.length} chars>") {
            it.copy(phrase = change.phrase)
        }
    }

    private fun update(change: String, transform: (ScreenTimeLockSettings) -> ScreenTimeLockSettings) {
        viewModelScope.launch {
            settings.update(change) { it.copy(screenTimeLock = transform(it.screenTimeLock)) }
            lock.refresh()
        }
    }

    companion object {
        /** Appendix A: 30 min–8 h in 30 min steps; phrase 8–40 characters. */
        const val CAP_MIN = 30
        const val CAP_MAX = 480
        const val CAP_STEP = 30
        val PHRASE_LENGTH = 8..40
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
