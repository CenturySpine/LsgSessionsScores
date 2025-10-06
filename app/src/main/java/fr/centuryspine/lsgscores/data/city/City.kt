package fr.centuryspine.lsgscores.data.city

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class City(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String
)