package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lsgscores.data.weather.WeatherInfo
import fr.centuryspine.lsgscores.data.serialization.LocalDateTimeAsStringSerializer
import fr.centuryspine.lsgscores.data.serialization.WeatherInfoAsJsonStringSerializer
import java.time.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

@Entity(
    tableName = "sessions",
        indices = [Index(value = ["gameZoneId"])]
)
@Serializable
data class Session(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,

    @SerialName("datetime")
    @kotlinx.serialization.Serializable(with = LocalDateTimeAsStringSerializer::class)
    val dateTime: LocalDateTime,

    @SerialName("enddatetime")
    @kotlinx.serialization.Serializable(with = LocalDateTimeAsStringSerializer::class)
    val endDateTime: LocalDateTime? = null,

    @SerialName("sessiontype")
    val sessionType: SessionType,

    @SerialName("scoringmodeid")
    val scoringModeId: Int,

    @SerialName("gamezoneid")
    val gameZoneId: Long,

    @SerialName("comment")
    val comment: String? = null,

    @SerialName("isongoing")
    val isOngoing: Boolean = false,

    @SerialName("weatherdata")
    @kotlinx.serialization.Serializable(with = WeatherInfoAsJsonStringSerializer::class)
    val weatherData: WeatherInfo? = null,

    @SerialName("cityid")
    val cityId: Long = 1
)
