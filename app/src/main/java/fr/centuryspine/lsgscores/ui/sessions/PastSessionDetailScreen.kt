package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel

/**
 * Read-only screen to display the details of a past (completed) session.
 * For now, this screen is intentionally empty as per requirements. Navigation to it must work.
 * No UI strings are displayed to avoid adding localization prematurely.
 */
@Composable
fun PastSessionDetailScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    sessionId: Long
) {
    // TODO: Implement read-only session details (no QR, no edit/validate/cancel, no score inputs)
    Box(modifier = Modifier.fillMaxSize())
}
