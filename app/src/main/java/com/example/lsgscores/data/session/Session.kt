package com.example.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val player1Id: Int,
    val player2Id: Int? = null // Null means solo team (individual session)
)

enum class SessionType {
    INDIVIDUAL,
    TEAM
}

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val dateTime: LocalDateTime,
    val sessionType: SessionType,           // Individual or Team, see enum below
    val scoringModeId: Int,                 // Foreign key to ScoringMode
    val comment: String? = null,
    val mediaUris: List<String> = emptyList() // List of photo/video URIs
    // Teams and holes played will be handled by relationship tables (not embedded here)
)