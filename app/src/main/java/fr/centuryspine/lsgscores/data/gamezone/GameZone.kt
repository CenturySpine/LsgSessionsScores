package fr.centuryspine.lsgscores.data.gamezone

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(
    tableName = "game_zones",
    indices = [Index(value = ["name"], unique = true)]
)
@Serializable
data class GameZone(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String,
    @SerialName("cityid")
    val cityId: Long = 1
)
