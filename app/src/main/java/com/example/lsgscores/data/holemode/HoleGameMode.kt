package com.example.lsgscores.data.holemode

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hole_game_modes")
data class HoleGameMode(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String
)