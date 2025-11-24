package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.ui.sessions.components.SessionHeaderBanner
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel

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
        }
        // Further read-only details will be added later (scores, standings, etc.)
    }
}
