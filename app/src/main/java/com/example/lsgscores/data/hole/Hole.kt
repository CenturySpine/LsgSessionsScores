package com.example.lsgscores.data.hole

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.lsgscores.data.gamezone.GameZone

@Entity(
    tableName = "holes",
    foreignKeys = [
        ForeignKey(entity = GameZone::class,
                   parentColumns = ["id"],
                   childColumns = ["gameZoneId"],
                   onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["gameZoneId"])]
)
data class Hole(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gameZoneId: Long, // Replaced geoZone with gameZoneId
    val description: String?,
    val constraints: String?,
    val distance: Int?,
    val par: Int,
    @Embedded(prefix = "start_") val start: HolePoint,
    @Embedded(prefix = "end_") val end: HolePoint
)
