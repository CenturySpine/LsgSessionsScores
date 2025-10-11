package fr.centuryspine.lsgscores.data.session


import com.example.lsgscores.data.weather.WeatherInfo
import fr.centuryspine.lsgscores.data.serialization.LocalDateTimeAsStringSerializer
import fr.centuryspine.lsgscores.data.serialization.WeatherInfoAsJsonStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime


@Serializable
data class Session(

    @SerialName("id")
    val id: Long = 0,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("datetime")
    @Serializable(with = LocalDateTimeAsStringSerializer::class)
    val dateTime: LocalDateTime,

    @SerialName("enddatetime")
    @Serializable(with = LocalDateTimeAsStringSerializer::class)
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
    @Serializable(with = WeatherInfoAsJsonStringSerializer::class)
    val weatherData: WeatherInfo? = null,

    @SerialName("cityid")
    val cityId: Long = 1
)
