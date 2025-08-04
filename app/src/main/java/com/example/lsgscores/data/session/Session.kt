package com.example.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,               // FK vers Session
    val player1Id: Int,               // FK User, obligatoire
    val player2Id: Int? = null        // FK User, optionnel (solo ou équipe)
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
    val sessionType: SessionType,     // Individual or Team
    val scoringModeId: Int,           // Foreign key to ScoringMode
    val comment: String? = null
    // PLUS DE mediaUris ICI !
    // Les médias sont stockés dans la table Media, liés via sessionId.
    // Les équipes sont gérées par l'entité Team avec sessionId en FK.
)