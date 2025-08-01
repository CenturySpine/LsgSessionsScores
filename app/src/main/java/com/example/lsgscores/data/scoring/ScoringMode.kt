package com.example.lsgscores.data.scoring

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scoring_modes")
data class ScoringMode(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String
)
