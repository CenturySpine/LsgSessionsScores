package fr.centuryspine.lsgscores.ui.sessions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Reusable banner for session details: Date, Scoring mode and optional QR entry.
 * This mirrors the visuals used in OngoingSessionScreen (original block lines 227–305).
 * The QR card is part of the component but can be hidden based on context (e.g., past session).
 */
@Composable
fun SessionHeaderBanner(
    dateTime: LocalDateTime,
    scoringModeLabel: String?,
    onScoringModeClick: (() -> Unit)? = null,
    showQr: Boolean,
    onQrClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date card (left)
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = dateTime.format(
                        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Scoring mode card (center)
        val scoringClickable = scoringModeLabel != null && onScoringModeClick != null
        if (scoringClickable) {
            Card(
                onClick = { onScoringModeClick?.invoke() },
                enabled = true,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scoringModeLabel!!,
                        style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (scoringModeLabel != null) {
                        Text(
                            text = scoringModeLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // QR card (right)
        if (showQr && onQrClick != null) {
            Card(
                onClick = onQrClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null, // Avoid hard-coded non-localized text
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
