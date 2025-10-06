package fr.centuryspine.lsgscores.data.session


import fr.centuryspine.lsgscores.data.player.Player

data class TeamWithPlayers(
    val team: Team,
    val player1: Player?,
    val player2: Player?
)