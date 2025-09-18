package fr.centuryspine.lsgscores.data.player

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "players")
@Serializable
data class Player(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String,
    @SerialName("photouri")
    val photoUri: String? = null,
    @SerialName("cityid")
    val cityId: Long = 1
)
