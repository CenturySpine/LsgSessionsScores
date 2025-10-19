package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.ui.BottomNavItem
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel

@Composable
fun JoinSessionTeamPickerScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    sessionId: Long
) {
    val session = sessionViewModel.getSessionById(sessionId).collectAsState(initial = null).value
    val teamsWithPlayers = sessionViewModel.getTeamsWithPlayersForSession(sessionId).collectAsState(initial = emptyList()).value

    var selectedTeamId by remember { mutableStateOf<Long?>(null) }

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
