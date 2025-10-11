package fr.centuryspine.lsgscores.data.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class PlayedHole(

    @SerialName("id")
    val id: Long = 0,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("sessionid")
    val sessionId: Long,      // FK to Session
    @SerialName("holeid")
    val holeId: Long,         // FK to Hole (from the repository)
    @SerialName("gamemodeid")
    val gameModeId: Int,      // FK to HoleGameMode (e.g., Greensome, Scramble, etc.)
    @SerialName("position")
    val position: Int         // The order of the hole in the session (1, 2, 3, ...)
)
