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

/** The stored session, if any; kept apart from the settings (and the backup), so it survives restarts. */
@Serializable
data class FocusState(val session: FocusSession? = null)

/** Starts and ends focus sessions and tells when one is running (F9). Start and end are logged (F17.7). */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusRepository(private val dataStore: DataStore<FocusState>, private val clock: Clock, private val logger: Logger) {

    /** The running session, or null. Ends on its own when the time is up: one timer, no polling (NFR-P1). */
    val active: Flow<FocusSession?> = dataStore.data.map { it.session }.distinctUntilChanged().transformLatest { session ->
        val left = session?.let { it.endsAtMillis - clock.millis() } ?: 0
        if (session == null || left <= 0) {
            emit(null)
        } else {
            emit(session)
            delay(left)
            logger.info(TAG, "focus ended: time up")
            emit(null)
        }
    }

    suspend fun start(minutes: Int, allowed: Set<String>) {
        val now = clock.millis()
        dataStore.updateData { FocusState(FocusSession(now, now + minutes * MILLIS_PER_MINUTE, allowed)) }
        logger.info(TAG, "focus started: $minutes min, ${allowed.size} apps allowed")
    }

    /** Ends the session early (F9.5). */
    suspend fun end() {
        if (dataStore.data.first().session == null) return
        dataStore.updateData { FocusState() }
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
