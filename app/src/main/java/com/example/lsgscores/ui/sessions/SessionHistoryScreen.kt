package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lsgscores.R
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.viewmodel.SessionViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessionViewModel: SessionViewModel
) {
    val completedSessions by sessionViewModel.completedSessions.collectAsState()

    Scaffold(
        topBar = {

        }
    ) { paddingValues ->
        if (completedSessions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.session_history_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.session_history_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Sessions list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(completedSessions) { session ->
                    SessionHistoryCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date formatting using localized format from resources
            val currentLocale = Locale.getDefault()
            val datePattern = stringResource(R.string.session_history_date_format_pattern)

            val formattedDate = session.dateTime.format(
                DateTimeFormatter.ofPattern(datePattern, currentLocale)
            )
            val formattedTime = session.dateTime.format(
                DateTimeFormatter.ofPattern("HH:mm", currentLocale)
            )

            // Capitalize first letter appropriately for each language
            val displayDate = if (currentLocale.language == "fr") {
                formattedDate.replaceFirstChar { it.uppercase() }
            } else {
                formattedDate // English dates are already properly capitalized
            }

            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.session_history_time_prefix)} $formattedTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Display duration if session is completed
                session.endDateTime?.let { endTime ->
                    val duration = Duration.between(session.dateTime, endTime)
                    val hours = duration.toHours()
                    val minutes = duration.toMinutes() % 60

                    val durationText = when {
                        hours > 0 -> {
                            if (minutes > 0) {
                                stringResource(R.string.session_history_duration_hours_minutes, hours, minutes)
                            } else {
                                stringResource(R.string.session_history_duration_hours, hours)
                            }
                        }
                        else -> stringResource(R.string.session_history_duration_minutes, minutes)
                    }

                    Text(
                        text = stringResource(R.string.session_history_separator),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${stringResource(R.string.session_history_duration_prefix)} $durationText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}