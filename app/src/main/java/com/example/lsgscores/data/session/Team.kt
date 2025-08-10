package com.example.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,               // FK vers Session
    val player1Id: Long,               // FK User, obligatoire
    val player2Id: Long? = null        // FK User, optionnel (solo ou équipe)
)