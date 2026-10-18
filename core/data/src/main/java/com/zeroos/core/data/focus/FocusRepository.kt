package com.zeroos.core.data.focus

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.error
import com.zeroos.core.logging.info
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/** A running focus session (F9.2–F9.4): until [endsAtMillis], only [allowed] apps (app keys) are shown or opened. */
@Serializable
data class FocusSession(val startedAtMillis: Long, val endsAtMillis: Long, val allowed: Set<String>) {
    fun allows(appKey: String) = appKey in allowed
}

/**
 * The stored session, if any; kept apart from the settings (and the backup), so it survives restarts.
 * [dndRestore] is the Do Not Disturb filter to put back when focus ends (F9.7), set only if focus turned it on.
 */
@Serializable
data class FocusState(val session: FocusSession? = null, val dndRestore: Int? = null)

/**
 * Starts and ends focus sessions and tells when one is running (F9). Start and end are logged (F17.7). If focus
 * turned Do Not Disturb on, it's put back as it was when focus ends, even after a restart (F9.7).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusRepository(
    private val dataStore: DataStore<FocusState>,
    private val clock: Clock,
    private val logger: Logger,
    private val dnd: DoNotDisturb = DoNotDisturb.Unavailable
) {

    /** The running session, or null. Ends on its own when the time is up: one timer, no polling (NFR-P1). */
    val active: Flow<FocusSession?> = dataStore.data.map { it.session }.distinctUntilChanged().transformLatest { session ->
        val left = session?.let { it.endsAtMillis - clock.millis() } ?: 0
        if (session == null || left <= 0) {
            restoreDnd()
            emit(null)
        } else {
            emit(session)
            delay(left)
            logger.info(TAG, "focus ended: time up")
            restoreDnd()
            emit(null)
        }
    }

    /** [turnOnDnd]: the Do Not Disturb setting (F9.7); it's skipped, and logged, without access. */
    suspend fun start(minutes: Int, allowed: Set<String>, turnOnDnd: Boolean = false) {
        val now = clock.millis()
        val restore = if (turnOnDnd) turnOnDnd() else null
        dataStore.updateData { old ->
            FocusState(FocusSession(now, now + minutes * MILLIS_PER_MINUTE, allowed), restore ?: old.dndRestore)
        }
        logger.info(TAG, "focus started: $minutes min, ${allowed.size} apps allowed")
    }

    /** Turns Do Not Disturb on and returns the filter to restore, or null if it was already on or there's no access. */
    private fun turnOnDnd(): Int? {
        val before = if (dnd.hasAccess()) dnd.current() else null
        when {
            before == null -> logger.info(TAG, "do not disturb skipped: no access")

            before != DoNotDisturb.ON -> {
                dnd.set(DoNotDisturb.ON)
                logger.info(TAG, "do not disturb on")
            }
        }
        return before?.takeIf { it != DoNotDisturb.ON }
    }

    private suspend fun restoreDnd() {
        val restore = dataStore.data.first().dndRestore ?: return
        if (dnd.hasAccess()) {
            dnd.set(restore)
            logger.info(TAG, "do not disturb restored")
        }
        dataStore.updateData { it.copy(dndRestore = null) }
    }

    /** Ends the session early (F9.5). */
    suspend fun end() {
        if (dataStore.data.first().session == null) return
        dataStore.updateData { it.copy(session = null) }
        logger.info(TAG, "focus ended early")
    }

    private companion object {
        const val TAG = "focus"
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/** JSON for [FocusState]; a corrupt file is replaced (losing a running session is harmless). */
object FocusStateSerializer : Serializer<FocusState> {
    override val defaultValue = FocusState()

    override suspend fun readFrom(input: InputStream): FocusState = try {
        SettingsSerializer.json.decodeFromString(FocusState.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Focus state is not valid JSON", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Focus state has invalid values", e)
    }

    override suspend fun writeTo(t: FocusState, output: OutputStream) {
        output.write(SettingsSerializer.json.encodeToString(FocusState.serializer(), t).encodeToByteArray())
    }
}

fun createFocusStateDataStore(context: Context, logger: Logger, scope: CoroutineScope): DataStore<FocusState> =
    DataStoreFactory.create(
        serializer = FocusStateSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { e ->
            logger.error("focus", "focus state corrupted; reset", e)
            FocusState()
        },
        scope = scope,
        produceFile = { context.dataStoreFile("focus_state.json") }
    )
