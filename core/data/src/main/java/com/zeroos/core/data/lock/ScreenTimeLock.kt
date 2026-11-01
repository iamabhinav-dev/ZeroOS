package com.zeroos.core.data.lock

import androidx.datastore.core.DataStore
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wellbeing.WellbeingState
import com.zeroos.core.data.wellbeing.startOfDay
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.ScreenTimeLockSettings
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the screen-time lock means right now (F19). */
data class LockState(
    val enabled: Boolean = false,
    /** Today's cap is reached and no 15-minute unlock is running: only essential apps work (F19.3). */
    val active: Boolean = false,
    val usedMillis: Long = 0,
    val capMinutes: Int = 0,
    /** Essential apps as app keys (F19.3). */
    val essentialKeys: Set<String> = emptySet(),
    /** Packages that always work: essential apps' packages plus the phone app, Android Settings and ZeroOS (F19.3, F19.8). */
    val allowedPackages: Set<String> = emptySet(),
    /** End of a running 15-minute unlock, or 0 (F19.4). */
    val unlockedUntil: Long = 0
) {
    /** Whether the app with [key] (from home or the app list) may open now. */
    fun allowsKey(key: String): Boolean = !active || key in essentialKeys || AppKey.packageOf(key) in allowedPackages

    /** Whether [packageName] (in front, outside ZeroOS) may stay on screen now. */
    fun allowsPackage(packageName: String): Boolean = !active || packageName in allowedPackages
}

/** The running 15-minute unlock, kept with wellbeing's state so it survives restarts and stays out of the backup. */
class LockUnlockStore(private val dataStore: DataStore<WellbeingState>) {
    suspend fun unlockedUntil(): Long = dataStore.data.first().lockUnlockedUntil

    suspend fun unlockUntil(millis: Long) {
        dataStore.updateData { it.copy(lockUnlockedUntil = millis) }
    }
}

/**
 * The screen-time lock (F19, D-062). [refresh] reads today's screen time (counted as in F10, F19.2) and works out
 * whether the lock is on; it runs when ZeroOS comes to the front, before a launch and from the Accessibility
 * service. One timer at a time brings the lock back when an unlock ends or a new day starts (NFR-P1, no polling).
 */
class ScreenTimeLock(
    private val settings: SettingsRepository,
    private val todayMillis: suspend () -> Long?,
    private val unlocks: LockUnlockStore,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val logger: Logger,
    /** The phone app, Android Settings and ZeroOS: never locked (F19.3, F19.8). */
    private val alwaysAllowed: () -> Set<String>
) {
    var zone: () -> ZoneId = ZoneId::systemDefault
    private val mutableState = MutableStateFlow(LockState())
    val state: StateFlow<LockState> = mutableState.asStateFlow()
    private var timer: Job? = null

    /** Works out the lock's state now and returns it. */
    suspend fun refresh(): LockState {
        val lock = settings.settings.first().screenTimeLock
        val next = if (lock.enabled) stateFor(lock) else LockState()
        if (next.active != mutableState.value.active) {
            logger.info(TAG, if (next.active) "lock on: cap of ${lock.capMinutes} min reached" else "lock off")
        }
        mutableState.value = next
        scheduleNextChange(next)
        return next
    }

    /** F19.4: the right phrase unlocks for 15 minutes; a wrong one changes nothing. The typed text is never logged. */
    suspend fun unlock(typed: String): Boolean {
        val phrase = settings.settings.first().screenTimeLock.phrase
        if (typed != phrase) {
            logger.info(TAG, "lock: wrong phrase")
            return false
        }
        unlocks.unlockUntil(clock.millis() + UNLOCK_MILLIS)
        logger.info(TAG, "lock: unlocked for ${UNLOCK_MILLIS / MILLIS_PER_MINUTE} min")
        refresh()
        return true
    }

    /** True when [typed] is the current phrase, for changes that weaken the lock (F19.6). Never logged. */
    suspend fun isPhrase(typed: String): Boolean = typed == settings.settings.first().screenTimeLock.phrase

    private suspend fun stateFor(lock: ScreenTimeLockSettings): LockState {
        val used = todayMillis()
        if (used == null) logger.warn(TAG, "lock: screen time not readable; not locking")
        val now = clock.millis()
        val unlockedUntil = unlocks.unlockedUntil().takeIf { it > now } ?: 0
        val capReached = used != null && used >= lock.capMinutes * MILLIS_PER_MINUTE
        return LockState(
            enabled = true,
            active = capReached && unlockedUntil == 0L,
            usedMillis = used ?: 0,
            capMinutes = lock.capMinutes,
            essentialKeys = lock.essentialApps.toSet(),
            allowedPackages = lock.essentialApps.map(AppKey::packageOf).toSet() + alwaysAllowed(),
            unlockedUntil = unlockedUntil
        )
    }

    /** While locked or unlocked, check again when the unlock ends or at midnight, whichever is first. */
    private fun scheduleNextChange(state: LockState) {
        timer?.cancel()
        if (!state.enabled || (!state.active && state.unlockedUntil == 0L)) return
        val now = clock.instant()
        val midnight = startOfDay(now.atZone(zone()).plusDays(1).toInstant(), zone()).toEpochMilli()
        val at = listOf(midnight, state.unlockedUntil).filter { it > now.toEpochMilli() }.min()
        timer = scope.launch {
            delay(at - clock.millis())
            refresh()
        }
    }

    companion object {
        private const val TAG = "lock"
        private const val MILLIS_PER_MINUTE = 60_000L

        /** F19.4, Appendix A: one unlock lasts 15 minutes. */
        const val UNLOCK_MILLIS = 15 * MILLIS_PER_MINUTE
    }
}
