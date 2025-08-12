package com.example.lsgscores.data.media

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "media")

data class Media(
    val id: Long = 0,
    val sessionId: Long,     
    val uri: String,
    val comment: String? = null,
    val dateAdded: LocalDateTime = LocalDateTime.now()
)