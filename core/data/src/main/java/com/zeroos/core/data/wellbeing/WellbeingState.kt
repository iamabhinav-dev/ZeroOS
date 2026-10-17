package com.zeroos.core.data.wellbeing

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
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Wellbeing's running state, kept apart from the settings (and so out of the backup): today's "5 more minutes"
 * uses per app (F8.4) and the pun shuffle bags (F8.13). [day] is the local date the uses belong to (F8.6).
 */
@Serializable
data class WellbeingState(
    val day: String = "",
    val extrasUsed: Map<String, Int> = emptyMap(),
    val pauseBag: PunBag = PunBag(),
    val limitBag: PunBag = PunBag()
)

/** The two pun lists (F8.13): B for the pause, C for the limit (docs/puns.md). */
enum class PunList { PAUSE, LIMIT }

/** Reads and changes [WellbeingState]. The day follows the phone's current time zone ([zone] is read each time). */
class WellbeingStateRepository(
    private val dataStore: DataStore<WellbeingState>,
    private val clock: Clock,
    private val logger: Logger,
    private val random: Random = Random.Default,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) {
    /** "5 more minutes" uses per app today; a new day starts with none (F8.6). */
    suspend fun extrasUsedToday(): Map<String, Int> = dataStore.data.first().extrasToday(today())

    /** Records one "5 more minutes" for [packageName] (F8.4). */
    suspend fun useExtra(packageName: String) {
        val today = today()
        val state = dataStore.updateData { s ->
            val uses = s.extrasToday(today)
            s.copy(day = today, extrasUsed = uses + (packageName to (uses[packageName] ?: 0) + 1))
        }
        logger.info(TAG, "5 more minutes for $packageName (${state.extrasUsed[packageName]} today)")
    }

    /** Index of the next line of [list], which has [size] lines (F8.13). */
    suspend fun nextPun(list: PunList, size: Int): Int {
        var index = 0
        dataStore.updateData { s ->
            val (drawn, bag) = (if (list == PunList.PAUSE) s.pauseBag else s.limitBag).draw(size, random)
            index = drawn
            if (list == PunList.PAUSE) s.copy(pauseBag = bag) else s.copy(limitBag = bag)
        }
        return index
    }

    private fun today() = clock.instant().atZone(zone()).toLocalDate().toString()

    private fun WellbeingState.extrasToday(today: String) = if (day == today) extrasUsed else emptyMap()

    private companion object {
        const val TAG = "wellbeing"
    }
}

/** JSON for [WellbeingState]; corrupt files are replaced (see [createWellbeingStateDataStore]). */
object WellbeingStateSerializer : Serializer<WellbeingState> {
    override val defaultValue = WellbeingState()

    override suspend fun readFrom(input: InputStream): WellbeingState = try {
        SettingsSerializer.json.decodeFromString(WellbeingState.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Wellbeing state is not valid JSON", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Wellbeing state has invalid values", e)
    }

    override suspend fun writeTo(t: WellbeingState, output: OutputStream) {
        output.write(SettingsSerializer.json.encodeToString(WellbeingState.serializer(), t).encodeToByteArray())
    }
}

/** A corrupted file is logged and replaced: losing today's extra uses and pun order is harmless. */
fun createWellbeingStateDataStore(context: Context, logger: Logger, scope: CoroutineScope): DataStore<WellbeingState> =
    DataStoreFactory.create(
        serializer = WellbeingStateSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { e ->
            logger.error("wellbeing", "wellbeing state corrupted; reset", e)
            WellbeingState()
        },
        scope = scope,
        produceFile = { context.dataStoreFile("wellbeing_state.json") }
    )
