package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.ui.BottomNavItem
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel

@Composable
fun JoinSessionTeamPickerScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    authViewModel: AuthViewModel,
    sessionId: Long
) {
    val session = sessionViewModel.getSessionById(sessionId).collectAsState(initial = null).value
    val teamsWithPlayers =
        sessionViewModel.getTeamsWithPlayersForSession(sessionId).collectAsState(initial = emptyList()).value

    var selectedTeamId by remember { mutableStateOf<Long?>(null) }
    var attemptedAutoJoin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Rejoindre la session", style = MaterialTheme.typography.titleLarge)
        if (session == null) {
            Text("Session introuvable")
            TextButton(onClick = { navController.popBackStack() }) { Text("Retour") }
            return@Column
        }
        if (!session.isOngoing) {
            Text("Cette session n'est plus active")
            TextButton(onClick = { navController.popBackStack() }) { Text("Retour") }
            return@Column
        }

        // Auto-join if current user is linked to a player who is in one of the teams of this session
        LaunchedEffect(session?.id, teamsWithPlayers.size, attemptedAutoJoin) {
            if (!attemptedAutoJoin && session != null && session.isOngoing && teamsWithPlayers.isNotEmpty()) {

                val linkedPlayerId = authViewModel.getLinkedPlayerIdForCurrentUser()
                if (linkedPlayerId != null) {
                    val matchingTeam = teamsWithPlayers.firstOrNull { twp ->
                        (twp.player1?.id == linkedPlayerId) || (twp.player2?.id == linkedPlayerId)
                    }
                    if (matchingTeam != null) {
                        // Set participant context and navigate directly to ongoing session
                        sessionViewModel.setParticipantMode(true)
                        sessionViewModel.setParticipantSession(sessionId)
                        sessionViewModel.setParticipantTeam(matchingTeam.team.id)
                        sessionViewModel.forceSelectCity(session.cityId)
                        navController.navigate(BottomNavItem.OngoingSession.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        return@LaunchedEffect
                    }
                    attemptedAutoJoin = true
                }
                // If no match or no link, leave UI as-is for manual selection
            }
        }

        Text("Choisissez votre équipe/joueur", style = MaterialTheme.typography.titleMedium)
        teamsWithPlayers.forEach { teamWithPlayers ->
            val name = listOfNotNull(teamWithPlayers.player1?.name, teamWithPlayers.player2?.name).joinToString(" & ")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = selectedTeamId == teamWithPlayers.team.id,
                    onClick = { selectedTeamId = teamWithPlayers.team.id }
                )
                Text(text = if (name.isBlank()) "Equipe ${teamWithPlayers.team.id}" else name)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) { Text("Annuler") }
            Button(
                onClick = {
                    val teamId = selectedTeamId
                    if (teamId != null) {
                        sessionViewModel.setParticipantMode(true)
                        sessionViewModel.setParticipantSession(sessionId)
                        sessionViewModel.setParticipantTeam(teamId)
                        sessionViewModel.forceSelectCity(session.cityId)
                        navController.navigate(BottomNavItem.OngoingSession.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                enabled = selectedTeamId != null,
                modifier = Modifier.weight(1f)
            ) { Text("Rejoindre") }
        }
    }
}
