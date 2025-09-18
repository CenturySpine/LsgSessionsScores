package fr.centuryspine.lsgscores.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "teams")
@Serializable
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,               // FK vers Session
    val player1Id: Long,               // FK User, obligatoire
    val player2Id: Long? = null        // FK User, optionnel (solo ou équipe)
)