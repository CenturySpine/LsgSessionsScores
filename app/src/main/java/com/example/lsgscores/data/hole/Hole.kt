package com.example.lsgscores.data.hole

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded

@Entity(tableName = "holes")
data class Hole(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val geoZone: String,
    val description: String?,
    val constraints: String?,
    val distance: Int?,
    val par: Int,
    @Embedded(prefix = "start_") val start: HolePoint,
    @Embedded(prefix = "end_") val end: HolePoint
)
