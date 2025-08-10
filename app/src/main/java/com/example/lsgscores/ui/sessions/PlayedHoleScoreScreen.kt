package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.ui.common.NumberInputField
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

    val calculatedScores = sessionViewModel.computeScoresForCurrentScoringMode(strokesMap)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Enter the strokes for each team", style = MaterialTheme.typography.titleLarge)

        teamsWithPlayers.forEach { teamWithPlayers ->
            val playerNames =
                listOfNotNull(teamWithPlayers.player1?.name, teamWithPlayers.player2?.name)
                    .joinToString(" & ")

            val strokesValue = strokesByTeam[teamWithPlayers.team.id] ?: ""
            val liveScore = calculatedScores[teamWithPlayers.team.id] ?: 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NumberInputField(
                    value = strokesValue,
                    onValueChange = { newValue ->
                        strokesByTeam[teamWithPlayers.team.id] = newValue
                    },
                    label = playerNames,
                    modifier = Modifier.weight(1f),
                    minValue = 0
                )
                AssistChip(
                    onClick = { /* Read-only chip */ },
                    label = { Text("$liveScore") },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    sessionViewModel.deletePlayedHole(playedHoleId) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

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
                    navController.popBackStack()
                },
                enabled = teams.all { strokesByTeam[it.id]?.isNotEmpty() == true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save scores")
            }
        }
    }
}
