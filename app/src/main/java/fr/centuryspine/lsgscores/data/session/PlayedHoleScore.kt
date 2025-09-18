package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "played_hole_scores")
@Serializable
data class PlayedHoleScore(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,

    @SerialName("playedholeid")
    val playedHoleId: Long,     // FK to PlayedHole
    @SerialName("teamid")
    val teamId: Long,           // FK to Team (or Player, adapt if needed)
    @SerialName("strokes")
    val strokes: Int            // Number of strokes entered by the user
)
