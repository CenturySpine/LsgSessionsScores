package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lsgscores.data.weather.WeatherInfo
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import java.time.LocalDateTime

@Entity(
    tableName = "sessions",
        indices = [Index(value = ["gameZoneId"])]
)
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: LocalDateTime,        // This becomes startDateTime conceptually
    val endDateTime: LocalDateTime? = null,  // New field - null while ongoing
    val sessionType: SessionType,
    val scoringModeId: Int,
    val gameZoneId: Long, // New field for GameZone
    val comment: String? = null,
    val isOngoing: Boolean = false,
    val weatherData: WeatherInfo? = null
)
