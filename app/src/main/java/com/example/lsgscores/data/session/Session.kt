package com.example.lsgscores.data.session

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.lsgscores.data.player.Player
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
    val dateTime: LocalDateTime,        // This becomes startDateTime conceptually
    val endDateTime: LocalDateTime? = null,  // New field - null while ongoing
    val sessionType: SessionType,
    val scoringModeId: Int,
    val comment: String? = null,
    val isOngoing: Boolean = false
)
data class TeamWithPlayers(
    @Embedded val team: Team,
    @Relation(
        parentColumn = "player1Id",
        entityColumn = "id"
    )
    val player1: Player?,
    @Relation(
        parentColumn = "player2Id",
        entityColumn = "id"
    )
    val player2: Player?
)