package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "teams")
@Serializable
data class Team(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,
    @SerialName("sessionid")
    val sessionId: Long,               // FK vers Session
    @SerialName("player1id")
    val player1Id: Long,               // FK User, obligatoire
    @SerialName("player2id")
    val player2Id: Long? = null        // FK User, optionnel (solo ou équipe)
)