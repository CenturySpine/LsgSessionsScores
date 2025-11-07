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
    val effectiveTeamId by sessionViewModel.effectiveUserTeamId.collectAsState(initial = null)

    // Collect teams with players for the current session/played hole (single source of truth)
    val teamsWithPlayers by sessionViewModel
        .getTeamsWithPlayersForPlayedHole(playedHoleId)
        .collectAsState(initial = emptyList())

    // Admin: which other teams are expanded (visible). Own team is always visible.
    val expandedTeams = remember { mutableStateListOf<Long>() }

    // Compute the set of team IDs considered visible for input and saving
    val visibleTeamIds: Set<Long> = when {
        isParticipant && participantTeamId != null -> setOf(participantTeamId!!)
        !isParticipant && effectiveTeamId != null -> setOf(effectiveTeamId!!) + expandedTeams.toSet()
        else -> teamsWithPlayers.map { it.team.id }.toSet() // Fallback when admin's team is unknown
    }

    // State for strokes entered by user, one selection per team ("0".."10" or "X"), no default
    val strokesByTeam = remember { mutableStateMapOf<Long, String?>() }

    // Prefill from existing scores in DB for this played hole, but only for visible teams
    val existingScores by sessionViewModel
        .getScoresForPlayedHole(playedHoleId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(existingScores, visibleTeamIds) {
        // Map DB strokes to selection labels ("X" for 10)
        val byTeam = existingScores.associate { it.teamId to it.strokes }
        visibleTeamIds.forEach { teamId ->
            if (strokesByTeam[teamId] == null) {
                val s = byTeam[teamId]
                if (s != null) {
                    strokesByTeam[teamId] = if (s >= 10) "X" else s.toString()
                }
            }
        }
    }

    // Convert current selection into a map for the calculator ("X" counts as 10), only for visible teams
    val strokesMap = visibleTeamIds.associateWith { teamId ->
        val sel = strokesByTeam[teamId]
        if (sel == "X") 10 else sel?.toIntOrNull() ?: 0
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

        // In participant mode, hide all other teams entirely (no header). Admins see all headers
        val teamsToRender = if (isParticipant && participantTeamId != null) {
            teamsWithPlayers.filter { it.team.id == participantTeamId }
        } else {
            teamsWithPlayers
        }

        teamsToRender.forEach { teamWithPlayers ->
            val teamId = teamWithPlayers.team.id
            val playerNames =
                listOfNotNull(teamWithPlayers.player1?.name, teamWithPlayers.player2?.name)
                    .joinToString(" & ")

            val selectedLabel = strokesByTeam[teamId]
            val liveScore = calculatedScores[teamId] ?: 0

            // Determine visibility for this team
            val isOwnTeam = if (isParticipant) participantTeamId == teamId else effectiveTeamId == teamId
            val isVisible = when {
                isParticipant -> participantTeamId == teamId
                effectiveTeamId == null -> true // Fallback: show all when we don't know admin's team
                isOwnTeam -> true
                else -> expandedTeams.contains(teamId)
            }

            // Header with toggle for admin on other teams
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(playerNames, style = MaterialTheme.typography.bodyLarge)
                    if (!isParticipant && effectiveTeamId != null && !isOwnTeam) {
                        val expanded = expandedTeams.contains(teamId)
                        TextButton(onClick = {
                            if (expanded) expandedTeams.remove(teamId) else expandedTeams.add(teamId)
                        }) {
                            Text(if (expanded) "Masquer" else "Afficher")
                        }
                    }
                }

                if (isVisible) {
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
                                            val canEdit = if (isParticipant) {
                                                participantTeamId == teamId
                                            } else {
                                                // Admin: can edit own team and any expanded (visible) team
                                                effectiveTeamId == teamId || expandedTeams.contains(teamId)
                                            }
                                            if (canEdit) {
                                                // Toggle selection: ensure only one selected, allow deselect
                                                strokesByTeam[teamId] = if (isSelected) null else option
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
                        // Admin: only save visible teams (own + individually expanded)
                        visibleTeamIds.forEach { teamId ->
                            val sel = strokesByTeam[teamId]
                            val strokes = if (sel == "X") 10 else sel?.toIntOrNull()
                            if (strokes != null) {
                                sessionViewModel.savePlayedHoleScore(
                                    playedHoleId = playedHoleId,
                                    teamId = teamId,
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
                    // Admin can save with partial entries; enable when at least one VISIBLE team has a value
                    visibleTeamIds.any { teamId -> strokesByTeam[teamId] != null }
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
