package fr.centuryspine.lsgscores.data.serialization

import fr.centuryspine.lsgscores.data.DateTimeConverters
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

/**
 * Kotlinx serializer to represent LocalDateTime as ISO_LOCAL_DATE_TIME String,
 * matching Room converters and the Supabase schema choice (TEXT column).
 */
object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTimeAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val str = DateTimeConverters.fromLocalDateTime(value) ?: ""
        encoder.encodeString(str)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val str = decoder.decodeString()
        return DateTimeConverters.toLocalDateTime(str)
            ?: throw IllegalStateException("Invalid LocalDateTime string: $str")
    }
}
