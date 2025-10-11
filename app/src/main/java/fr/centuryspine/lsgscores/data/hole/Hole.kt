package fr.centuryspine.lsgscores.data.hole

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Hole(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("name")
    val name: String,
    @SerialName("gamezoneid")
    val gameZoneId: Long,
    @SerialName("description")
    val description: String?,
    @SerialName("distance")
    val distance: Int?,
    @SerialName("par")
    val par: Int,
    @SerialName("startphotouri")
    val startPhotoUri: String?,
    @SerialName("endphotouri")
    val endPhotoUri: String?
)
