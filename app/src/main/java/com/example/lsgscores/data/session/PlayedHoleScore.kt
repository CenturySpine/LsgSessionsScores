package com.example.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "played_hole_scores")
data class PlayedHoleScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val playedHoleId: Long,     // FK to PlayedHole
    val teamId: Long,           // FK to Team (or Player, adapt if needed)
    val strokes: Int            // Number of strokes entered by the user
)
