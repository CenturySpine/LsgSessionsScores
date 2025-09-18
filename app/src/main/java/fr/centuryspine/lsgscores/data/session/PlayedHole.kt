package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "played_holes")
@Serializable
data class PlayedHole(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sessionId: Long,      // FK to Session
    val holeId: Long,         // FK to Hole (from the repository)
    val gameModeId: Int,      // FK to HoleGameMode (e.g., Greensome, Scramble, etc.)
    val position: Int         // The order of the hole in the session (1, 2, 3, ...)
)
