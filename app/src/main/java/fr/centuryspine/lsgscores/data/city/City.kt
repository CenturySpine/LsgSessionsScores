package fr.centuryspine.lsgscores.data.city

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(
    tableName = "cities",
    indices = [Index(value = ["name"], unique = true)]
)
@Serializable
data class City(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String
)