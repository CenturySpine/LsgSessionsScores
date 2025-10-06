package fr.centuryspine.lsgscores.data.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class PlayedHoleScore(

    @SerialName("id")
    val id: Long = 0,

    @SerialName("playedholeid")
    val playedHoleId: Long,     // FK to PlayedHole
    @SerialName("teamid")
    val teamId: Long,           // FK to Team (or Player, adapt if needed)
    @SerialName("strokes")
    val strokes: Int            // Number of strokes entered by the user
)
