package com.example.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

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
