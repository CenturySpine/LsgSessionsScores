package fr.centuryspine.lsgscores.data.scoring


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class ScoringMode(

    @SerialName("id")
    val id: Int = 0,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String
)
