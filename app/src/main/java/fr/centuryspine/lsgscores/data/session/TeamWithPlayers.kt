package fr.centuryspine.lsgscores.data.session

import androidx.room.Embedded
import androidx.room.Relation
import fr.centuryspine.lsgscores.data.player.Player

data class TeamWithPlayers(
    @Embedded val team: Team,
    @Relation(
        parentColumn = "player1Id",
        entityColumn = "id"
    )
    val player1: Player?,
    @Relation(
        parentColumn = "player2Id",
        entityColumn = "id"
    )
    val player2: Player?
)