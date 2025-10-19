package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayedHoleScoreScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    playedHoleId: Long
) {
    val isParticipant by sessionViewModel.isParticipantMode.collectAsState()
    val participantTeamId by sessionViewModel.participantTeamId.collectAsState(initial = null)
    // Collect teams with players for the current session/played hole (single source of truth)
    val teamsWithPlayers by sessionViewModel
        .getTeamsWithPlayersForPlayedHole(playedHoleId)
        .collectAsState(initial = emptyList())
    // For participants, only show their own team; admins see all teams
    val visibleTeams = if (isParticipant && participantTeamId != null) {
        teamsWithPlayers.filter { it.team.id == participantTeamId }
    } else {
        teamsWithPlayers
    }

    // State for strokes entered by user, one selection per team ("0".."10" or "X"), no default
    val strokesByTeam = remember { mutableStateMapOf<Long, String?>() }

    // Prefill from existing scores in DB for this played hole
    val existingScores by sessionViewModel
        .getScoresForPlayedHole(playedHoleId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(existingScores, visibleTeams) {
        // Map DB strokes to selection labels ("X" for 10)
        val byTeam = existingScores.associate { it.teamId to it.strokes }
        visibleTeams.forEach { teamWithPlayers ->
            val teamId = teamWithPlayers.team.id
            if (strokesByTeam[teamId] == null) {
                val s = byTeam[teamId]
                if (s != null) {
                    strokesByTeam[teamId] = if (s >= 10) "X" else s.toString()
                }
            }
        }
    }

    // Convert current selection into a map for the calculator ("X" counts as 10)
    val strokesMap = visibleTeams.associate { teamWithPlayers ->
        val sel = strokesByTeam[teamWithPlayers.team.id]
        val value = if (sel == "X") 10 else sel?.toIntOrNull() ?: 0
        teamWithPlayers.team.id to value
    }

    val calculatedScores = sessionViewModel.computeScoresForCurrentScoringMode(strokesMap)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.played_hole_score_title),
            style = MaterialTheme.typography.titleLarge
        )

        val options = (0..9).map { it.toString() } + "X"

        visibleTeams.forEach { teamWithPlayers ->
            val playerNames =
                listOfNotNull(teamWithPlayers.player1?.name, teamWithPlayers.player2?.name)
                    .joinToString(" & ")

            val selectedLabel = strokesByTeam[teamWithPlayers.team.id]
            val liveScore = calculatedScores[teamWithPlayers.team.id] ?: 0

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(playerNames, style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalMinimumInteractiveComponentEnforcement provides false
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            options.forEach { option ->
                                val isSelected = selectedLabel == option
                                CompactSelectableChip(
                                    label = option,
                                    selected = isSelected,
                                    onClick = {
                                        val canEdit = !isParticipant || (participantTeamId == teamWithPlayers.team.id)
                                        if (canEdit) {
                                            // Toggle selection: ensure only one selected, allow deselect
                                            strokesByTeam[teamWithPlayers.team.id] = if (isSelected) null else option
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    AssistChip(
                        onClick = { /* Read-only chip */ },
                        label = { Text(liveScore.toString()) },
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // Cancel should not delete the played hole; simply go back
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.played_hole_score_button_cancel))
            }

            Button(
                onClick = {
                    if (isParticipant && participantTeamId != null) {
                        val sel = strokesByTeam[participantTeamId!!]
                        val strokes = if (sel == "X") 10 else sel?.toIntOrNull()
                        if (strokes != null) {
                            sessionViewModel.savePlayedHoleScore(
                                playedHoleId = playedHoleId,
                                teamId = participantTeamId!!,
                                strokes = strokes
                            )
                        }
                    } else {
                        visibleTeams.forEach { teamWithPlayers ->
                            val sel = strokesByTeam[teamWithPlayers.team.id]
                            val strokes = if (sel == "X") 10 else sel?.toIntOrNull()
                            if (strokes != null) {
                                sessionViewModel.savePlayedHoleScore(
                                    playedHoleId = playedHoleId,
                                    teamId = teamWithPlayers.team.id,
                                    strokes = strokes
                                )
                            }
                        }
                    }
                    navController.popBackStack()
                },
                enabled = if (isParticipant) {
                    participantTeamId != null && strokesByTeam[participantTeamId] != null
                } else {
                    // Admin can save with partial entries; enable when at least one team has a value
                    visibleTeams.any { teamWithPlayers -> strokesByTeam[teamWithPlayers.team.id] != null }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.played_hole_score_button_save))
            }
        }
    }
}

@Composable
private fun CompactSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = if (selected) 1.dp else 0.dp,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
