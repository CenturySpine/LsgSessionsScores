package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.viewmodel.SessionViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayedHoleScoreScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    playedHoleId: Long
) {
    // Load teams for the current session/played hole
    val teams by sessionViewModel.getTeamsForPlayedHole(playedHoleId)
        .collectAsState(initial = emptyList())
    // If you want, you can also retrieve the hole and its name for context
    val teamsWithPlayers by sessionViewModel
        .getTeamsWithPlayersForPlayedHole(playedHoleId)
        .collectAsState(initial = emptyList())

    // State for strokes entered by user, one field per team
    val strokesByTeam = remember { mutableStateMapOf<Long, String>() }

    // Convert current strokes input into a map for the calculator
    val strokesMap = teamsWithPlayers.associate { teamWithPlayers ->
        teamWithPlayers.team.id to (strokesByTeam[teamWithPlayers.team.id]?.toIntOrNull() ?: 0)
    }

// Calcul live des scores
    val calculatedScores = sessionViewModel.computeScoresForCurrentScoringMode(strokesMap)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Enter the strokes for each team", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(8.dp))

        teamsWithPlayers.forEach { teamWithPlayers ->
            val playerNames =
                listOfNotNull(teamWithPlayers.player1?.name, teamWithPlayers.player2?.name)
                    .joinToString(" & ")

            val strokesValue = strokesByTeam[teamWithPlayers.team.id] ?: ""
            val liveScore = calculatedScores[teamWithPlayers.team.id]

            OutlinedTextField(
                value = strokesValue,
                onValueChange = { newValue ->
                    strokesByTeam[teamWithPlayers.team.id] = newValue.filter { it.isDigit() }
                },
                label = { Text(playerNames) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (liveScore != null) {
                        Text("Score: $liveScore")
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                teamsWithPlayers.forEach { teamWithPlayers ->
                    val strokes = strokesByTeam[teamWithPlayers.team.id]?.toIntOrNull()
                    if (strokes != null) {
                        sessionViewModel.savePlayedHoleScore(
                            playedHoleId = playedHoleId,
                            teamId = teamWithPlayers.team.id,
                            strokes = strokes
                        )
                    }
                }
                navController.popBackStack() // Return to ongoing session screen
            },
            enabled = teams.all { strokesByTeam[it.id]?.isNotEmpty() == true }
        ) {
            Text("Save scores")
        }
    }
}
