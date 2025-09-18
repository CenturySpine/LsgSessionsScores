package fr.centuryspine.lsgscores.data.gamezone

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "game_zones",
    indices = [Index(value = ["name"], unique = true)]
)
@Serializable
data class GameZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cityId: Long = 1
)
