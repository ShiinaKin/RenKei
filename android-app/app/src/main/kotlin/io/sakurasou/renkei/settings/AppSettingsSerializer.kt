package io.sakurasou.renkei.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal object AppSettingsSerializer : Serializer<AppSettings> {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings =
        try {
            json.decodeFromString(input.readBytes().decodeToString())
        } catch (exception: SerializationException) {
            throw CorruptionException("Unable to read app settings", exception)
        } catch (exception: IllegalArgumentException) {
            throw CorruptionException("Unable to read app settings", exception)
        }

    override suspend fun writeTo(
        t: AppSettings,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
