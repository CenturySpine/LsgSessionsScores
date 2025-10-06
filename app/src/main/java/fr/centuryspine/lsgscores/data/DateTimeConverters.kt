// data/DateTimeConverters.kt

package fr.centuryspine.lsgscores.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Legacy Room TypeConverters removed. Kept utility methods without Room annotations for potential reuse.
object DateTimeConverters {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @JvmStatic
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }

    @JvmStatic
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
}
