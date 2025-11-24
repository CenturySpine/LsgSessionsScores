package fr.centuryspine.lsgscores.ui.sessions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.session.TeamWithPlayers
import fr.centuryspine.lsgscores.viewmodel.PlayedHoleDisplay

/**
 * Reusable card rendering of a played hole with team scores.
 * Visuals and layout mirror the inline implementation originally in OngoingSessionScreen.
 * Interactions are controlled via nullable callbacks:
 * - onClick: if non-null, the whole card is clickable.
 * - onDelete: if non-null, a delete icon is shown on the right.
 *
 * All comments must be in English; UI strings are sourced from string resources.
 */
@Composable
fun PlayedHoleCard(
    playedHole: PlayedHoleDisplay,
    teamsForSession: List<TeamWithPlayers>,
    scoringModeId: Int?,
    isLatest: Boolean,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLatest) Modifier.border(
                    BorderStroke(2.dp, Color(0xFF10B981)),
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${stringResource(R.string.ongoing_session_hole_prefix)} ${playedHole.holeName} (${playedHole.gameModeName})",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    teamsForSession.forEach { teamWithPlayers ->
                        val name = listOfNotNull(
                            teamWithPlayers.player1?.name,
                            teamWithPlayers.player2?.name
                        ).joinToString(" & ")
                        // Avoid hard-coded localized fallback; use an id marker when names are empty
                        val displayName = name.ifBlank { "#${teamWithPlayers.team.id}" }
                        val result = playedHole.teamResults.find { it.teamName == name }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                color = if (result == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = ": ",
                                style = MaterialTheme.typography.bodySmall
                            )
                            // Display rule: in Stroke Play (scoring mode id = 1), only show the calculated score.
                            // For other modes, show both values as "strokes - calculatedScore".
                            val scoreText = when {
                                result == null -> "-"
                                scoringModeId == 1 -> "${result.calculatedScore}"
                                else -> "${result.strokes} - ${result.calculatedScore}"
                            }
                            Text(
                                text = scoreText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ongoing_session_delete_hole_icon_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
