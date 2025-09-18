package fr.centuryspine.lsgscores.data.scoring

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "scoring_modes")
@Serializable
data class ScoringMode(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Int = 0,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String
)
