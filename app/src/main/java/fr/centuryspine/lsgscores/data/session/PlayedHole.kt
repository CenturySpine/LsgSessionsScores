package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "played_holes")
@Serializable
data class PlayedHole(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,

    @SerialName("sessionid")
    val sessionId: Long,      // FK to Session
    @SerialName("holeid")
    val holeId: Long,         // FK to Hole (from the repository)
    @SerialName("gamemodeid")
    val gameModeId: Int,      // FK to HoleGameMode (e.g., Greensome, Scramble, etc.)
    @SerialName("position")
    val position: Int         // The order of the hole in the session (1, 2, 3, ...)
)
