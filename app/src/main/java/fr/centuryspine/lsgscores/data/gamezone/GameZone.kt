package fr.centuryspine.lsgscores.data.gamezone

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GameZone(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String,
    @SerialName("cityid")
    val cityId: Long = 1
)
