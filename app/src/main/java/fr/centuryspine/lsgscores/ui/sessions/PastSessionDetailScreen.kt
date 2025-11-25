package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.ui.components.WeatherSummaryRow
import fr.centuryspine.lsgscores.ui.sessions.components.CollapsibleStandingsCard
import fr.centuryspine.lsgscores.ui.sessions.components.PlayedHoleCard
import fr.centuryspine.lsgscores.ui.sessions.components.SessionHeaderBanner
import fr.centuryspine.lsgscores.utils.SessionFormatters
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Read-only screen to display the details of a past (completed) session.
 * This screen shows the same header banner as the ongoing session (date + scoring mode),
 * but without the QR code and without any editing capabilities.
 */
@Composable
fun PastSessionDetailScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    sessionId: Long
) {
    val context = LocalContext.current
    val completedSessions by sessionViewModel.completedSessions.collectAsStateWithLifecycle()
    val scoringModes by sessionViewModel.scoringModes.collectAsStateWithLifecycle()
    // Past session standings collected via dedicated ViewModel entry point
    val pastStandings by sessionViewModel
        .getStandingsForSession(sessionId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    // Teams for the session (used to render per-team scores per hole)
    val teamsForSession by sessionViewModel
        .getTeamsWithPlayersForSession(sessionId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    // Played holes with scores for this specific session
    val playedHoles by sessionViewModel
        .getPlayedHolesWithScoresForSession(sessionId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val session = completedSessions.firstOrNull { it.id == sessionId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        session?.let { past ->
            val scoringLabel = scoringModes
                .firstOrNull { it.id == past.scoringModeId }
                ?.getLocalizedName(context)

            // Reuse the common banner with QR hidden in this context
            SessionHeaderBanner(
                dateTime = past.dateTime,
                scoringModeLabel = scoringLabel,
                onScoringModeClick = null, // Read-only: no info dialog for now
                showQr = false,
                onQrClick = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Load game zone name by id using Hilt ViewModel
            val gameZoneViewModel: GameZoneViewModel = hiltViewModel()
            val gameZoneLabel by produceState<String?>(initialValue = null, key1 = past.gameZoneId) {
                value = try {
                    gameZoneViewModel.getGameZoneById(past.gameZoneId)?.name
                } catch (_: Exception) {
                    null
                }
            }

            // Info card: line 1 = zone name + start time; line 2 = duration (left) + weather (right)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // First line
                    val timeText = past.dateTime.format(
                        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                    )
                    val firstLine = listOfNotNull(
                        gameZoneLabel?.takeIf { it.isNotBlank() },
                        timeText
                    ).joinToString(" - ")
                    Text(
                        text = firstLine,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Second line
                    Row(modifier = Modifier.fillMaxWidth()) {
                        past.endDateTime?.let { endTime ->
                            val durationText = SessionFormatters.formatSessionDuration(
                                context,
                                past.dateTime,
                                endTime
                            )
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        past.weatherData?.let { weather ->
                            WeatherSummaryRow(weatherInfo = weather, iconSize = 32.dp)
                        }
                    }
                }
            }

            // Standings for the past session with collapsible/expandable behavior
            if (pastStandings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                CollapsibleStandingsCard(
                    standings = pastStandings,
                    initiallyExpanded = false
                )
            }

            // Played holes list (read-only): same visuals as ongoing but without click/delete
            if (playedHoles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = fr.centuryspine.lsgscores.R.string.ongoing_session_label_holes_played.let { resId ->
                        // Use stringResource indirectly to avoid adding new labels
                        androidx.compose.ui.res.stringResource(resId)
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                // Add a small space between the section title and the list
                Spacer(modifier = Modifier.height(8.dp))

                // Ensure vertical spacing between hole cards, like in the ongoing screen
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    playedHoles.asReversed().forEach { ph ->
                        PlayedHoleCard(
                            playedHole = ph,
                            teamsForSession = teamsForSession,
                            scoringModeId = past.scoringModeId,
                            // In past session details, do not highlight the latest hole
                            isLatest = false,
                            onClick = null,
                            onDelete = null
                        )
                    }
                }
            }
        }
        // Further read-only details will be added later (scores, standings, etc.)
    }
}
