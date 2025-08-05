package com.example.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,               // FK vers Session
    val player1Id: Long,               // FK User, obligatoire
    val player2Id: Long? = null        // FK User, optionnel (solo ou équipe)
)

enum class SessionType {
    INDIVIDUAL,
    TEAM
}

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateTime: LocalDateTime,
    val sessionType: SessionType,     // Individual or Team
    val scoringModeId: Int,           // Foreign key to ScoringMode
    val comment: String? = null,
    val isOngoing: Boolean = false // true if this session is the ongoing one
)