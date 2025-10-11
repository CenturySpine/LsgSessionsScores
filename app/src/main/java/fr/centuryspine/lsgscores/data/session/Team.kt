package fr.centuryspine.lsgscores.data.session


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class Team(

    @SerialName("id")
    val id: Long = 0,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("sessionid")
    val sessionId: Long,               // FK vers Session
    @SerialName("player1id")
    val player1Id: Long,               // FK User, obligatoire
    @SerialName("player2id")
    val player2Id: Long? = null        // FK User, optionnel (solo ou équipe)
)