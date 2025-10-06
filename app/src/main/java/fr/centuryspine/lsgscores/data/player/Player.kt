package fr.centuryspine.lsgscores.data.player

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Player(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String,
    @SerialName("photouri")
    val photoUri: String? = null,
    @SerialName("cityid")
    val cityId: Long = 1
)
