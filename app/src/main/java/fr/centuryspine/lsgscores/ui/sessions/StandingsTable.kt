package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.viewmodel.TeamStanding

@Composable
fun StandingsTable(
    standings: List<TeamStanding>,
    modifier: Modifier = Modifier,
    wrapInCard: Boolean = true,
    showTitle: Boolean = true
) {
    if (standings.isEmpty()) return

    if (wrapInCard) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            StandingsTableContent(standings = standings, showTitle = showTitle)
        }
    } else {
        StandingsTableContent(standings = standings, showTitle = showTitle)
    }
}

@Composable
private fun StandingsTableContent(
    standings: List<TeamStanding>,
    showTitle: Boolean
) {
    // Determine if we should show the strokes column (hide it in stroke play mode id = 1)
    val showStrokesColumn = standings.firstOrNull()?.scoringModeId != 1
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.standings_table_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.standings_table_header_pos),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )

            // Spacer identical to the one in StandingRow
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(R.string.standings_table_header_team),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (showStrokesColumn) {
                Text(
                    text = stringResource(R.string.standings_table_header_strokes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = stringResource(R.string.standings_table_header_score),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Standings rows
        standings.forEach { standing ->
            StandingRow(standing = standing)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Render a single row of the standings table
@Composable
private fun StandingRow(
    standing: TeamStanding,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position badge
        PositionBadge(
            position = standing.position,
            modifier = Modifier.width(40.dp)
        )

        // Spacer between position and team name
        Spacer(modifier = Modifier.width(12.dp))

        // Team name
        Text(
            text = standing.teamName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Total strokes (hidden in stroke play where strokes == score)
        if (standing.scoringModeId != 1) {
            Text(
                text = standing.totalStrokes.toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )
        }

        // Total score
        Text(
            text = standing.totalScore.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PositionBadge(
    position: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (position) {
        1 -> Color(0xFFFFD700) to Color.Black // Gold
        2 -> Color(0xFFC0C0C0) to Color.Black // Silver
        3 -> Color(0xFFCD7F32) to Color.White // Bronze
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}