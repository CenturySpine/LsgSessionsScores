package com.example.lsgscores.data.hole

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val gameZoneId: Long,
    val description: String?,
    val distance: Int?,
    val par: Int,
    val startPhotoUri: String?,
    val endPhotoUri: String?
)
