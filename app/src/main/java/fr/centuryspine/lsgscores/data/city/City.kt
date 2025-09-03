package fr.centuryspine.lsgscores.data.city

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cities",
    indices = [Index(value = ["name"], unique = true)]
)
data class City(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)