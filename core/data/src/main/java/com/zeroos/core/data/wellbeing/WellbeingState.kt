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
    val limitBag: PunBag = PunBag(),
    val breakBag: PunBag = PunBag(),
    val breakButtonBag: PunBag = PunBag(),
    /** Short-video blocker, for [day] (F16.8, F16.11, F16.14): time in short videos, blocks, "Allow" uses. */
    val shortFormMillis: Long = 0,
    val shortFormBlocks: Int = 0,
    val shortFormAllowUses: Int = 0,
    val shortsBag: PunBag = PunBag()
)

/** Today's short-video numbers (F16.8, F16.14). */
data class ShortFormDay(val millis: Long = 0, val blocks: Int = 0, val allowUses: Int = 0)

/** The pun lists in docs/puns.md: A short videos (F16.9), B pause, C limit (F8.13), D break and E break button (F8.16). */
enum class PunList { PAUSE, LIMIT, BREAK, BREAK_BUTTON, SHORTS }

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
            val t = s.onDay(today)
            t.copy(extrasUsed = t.extrasUsed + (packageName to (t.extrasUsed[packageName] ?: 0) + 1))
        }
        logger.info(TAG, "5 more minutes for $packageName (${state.extrasUsed[packageName]} today)")
    }

    /** Today's short-video time, blocks and "Allow 5 minutes" uses; a new day starts at zero (F16.8). */
    suspend fun shortFormToday(): ShortFormDay {
        val s = dataStore.data.first().onDay(today())
        return ShortFormDay(s.shortFormMillis, s.shortFormBlocks, s.shortFormAllowUses)
    }

    /** Adds to today's short-video time, blocks and "Allow 5 minutes" uses (F16.8, F16.11, F16.14). */
    suspend fun recordShortForm(millis: Long = 0, blocks: Int = 0, allowUses: Int = 0) {
        dataStore.updateData { s ->
            s.onDay(today()).let {
                it.copy(
                    shortFormMillis = it.shortFormMillis + millis.coerceAtLeast(0),
                    shortFormBlocks = it.shortFormBlocks + blocks,
                    shortFormAllowUses = it.shortFormAllowUses + allowUses
                )
            }
        }
    }

    /** Index of the next line of [list], which has [size] lines (F8.13). */
    suspend fun nextPun(list: PunList, size: Int): Int {
        var index = 0
        dataStore.updateData { s ->
            val (drawn, bag) = s.bag(list).draw(size, random)
            index = drawn
            when (list) {
                PunList.PAUSE -> s.copy(pauseBag = bag)
                PunList.LIMIT -> s.copy(limitBag = bag)
                PunList.BREAK -> s.copy(breakBag = bag)
                PunList.BREAK_BUTTON -> s.copy(breakButtonBag = bag)
                PunList.SHORTS -> s.copy(shortsBag = bag)
            }
        }
        return index
    }

    private fun today() = clock.instant().atZone(zone()).toLocalDate().toString()

    private fun WellbeingState.bag(list: PunList) = when (list) {
        PunList.PAUSE -> pauseBag
        PunList.LIMIT -> limitBag
        PunList.BREAK -> breakBag
        PunList.BREAK_BUTTON -> breakButtonBag
        PunList.SHORTS -> shortsBag
    }

    /** This state on [today]: the day's counters start again when the date changes (F8.6, F16.8). */
    private fun WellbeingState.onDay(today: String) =
        if (day ==
            today
        ) {
            this
        } else {
            copy(day = today, extrasUsed = emptyMap(), shortFormMillis = 0, shortFormBlocks = 0, shortFormAllowUses = 0)
        }

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
