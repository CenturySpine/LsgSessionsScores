package fr.centuryspine.lsgscores.data.scoring

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "scoring_modes")
@Serializable
data class ScoringMode(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String
)
