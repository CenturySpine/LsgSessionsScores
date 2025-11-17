package fr.centuryspine.lsgscores.data.serialization

import com.example.lsgscores.data.weather.WeatherInfo
import fr.centuryspine.lsgscores.data.WeatherConverters
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

/**
 * Serializer that stores WeatherInfo as a JSON string (consistent with Room converters
 * and existing migration logic). It also accepts either a JSON object or a JSON string
 * when deserializing, to be robust with what PostgREST returns.
 */
object WeatherInfoAsJsonStringSerializer : KSerializer<WeatherInfo> {
    private val conv = WeatherConverters()

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WeatherInfoAsJsonString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WeatherInfo) {
        val jsonText = conv.fromWeatherInfo(value) ?: "{}"
        encoder.encodeString(jsonText)
    }

    override fun deserialize(decoder: Decoder): WeatherInfo {
        return when (decoder) {
            is JsonDecoder -> {
                val elem: JsonElement = decoder.decodeJsonElement()
                val text = when {
                    elem is kotlinx.serialization.json.JsonPrimitive && elem.isString -> elem.content
                    else -> Json.encodeToString(JsonElement.serializer(), elem)
                }
                conv.toWeatherInfo(text) ?: WeatherInfo(
                    0,
                    "",
                    "",
                    0
                ) // fallback; structure will be overwritten by real data if present
            }

            else -> {
                val text = decoder.decodeString()
                conv.toWeatherInfo(text) ?: WeatherInfo(0, "", "", 0)
            }
        }
    }
}
