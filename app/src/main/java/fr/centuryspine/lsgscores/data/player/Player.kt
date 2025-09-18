package fr.centuryspine.lsgscores.data.player

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "players")
@Serializable
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoUri: String? = null , 
    val cityId: Long = 1
)
