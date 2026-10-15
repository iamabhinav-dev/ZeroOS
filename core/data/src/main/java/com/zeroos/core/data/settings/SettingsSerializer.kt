package com.zeroos.core.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zeroos.core.model.settings.LauncherSettings
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Reads and writes [LauncherSettings] as JSON (D-029). Also used by settings backup (F13). */
object SettingsSerializer : Serializer<LauncherSettings> {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        coerceInputValues = true
    }

    override val defaultValue: LauncherSettings = LauncherSettings()

    override suspend fun readFrom(input: InputStream): LauncherSettings = try {
        json.decodeFromString(LauncherSettings.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Settings file is not valid JSON", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Settings file has invalid values", e)
    }

    override suspend fun writeTo(t: LauncherSettings, output: OutputStream) {
        output.write(json.encodeToString(LauncherSettings.serializer(), t).encodeToByteArray())
    }
}
