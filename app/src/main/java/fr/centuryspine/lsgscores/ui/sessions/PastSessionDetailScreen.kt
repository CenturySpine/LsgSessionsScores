package fr.centuryspine.lsgscores.ui.sessions

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dagger.hilt.android.EntryPointAccessors
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.common.RemoteImage
import fr.centuryspine.lsgscores.ui.common.RemoteImageEntryPoint
import fr.centuryspine.lsgscores.ui.components.WeatherSummaryRow
import fr.centuryspine.lsgscores.ui.sessions.components.CollapsibleStandingsCard
import fr.centuryspine.lsgscores.ui.sessions.components.PlayedHoleCard
import fr.centuryspine.lsgscores.ui.sessions.components.SessionHeaderBanner
import fr.centuryspine.lsgscores.utils.SessionFormatters
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
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
            // Current authenticated user ID to check ownership (only owner can upload photos)
            val currentUserId = remember { sessionViewModel.currentUserIdOrNull() }
            val canUploadPhotos = currentUserId != null && past.userId == currentUserId
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

            // Upload photos to Supabase Storage (Sessions bucket) and display thumbnails next to the picker
            val coroutineScope = rememberCoroutineScope()
            // Trigger to reload the session photos after uploads complete
            val reloadTrigger = remember { mutableStateOf(0) }
            val pickMultipleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetMultipleContents()
            ) { uris: List<Uri> ->
                if (uris.isNotEmpty()) {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        RemoteImageEntryPoint::class.java
                    )
                    val storage = entryPoint.supabaseStorageHelper()
                    coroutineScope.launch {
                        uris.forEach { u ->
                            runCatching { storage.uploadSessionPhoto(sessionId, u) }
                                .onFailure { t -> Log.w("PastSession", "Upload failed for $u", t) }
                        }
                        // After finishing all uploads, trigger a refresh of the thumbnails
                        reloadTrigger.value++
                    }
                }
            }

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

            // Load all session photos from storage (Sessions/<sessionId>/...) and display as thumbnails
            val sessionPhotos by produceState(
                initialValue = emptyList<String>(),
                key1 = sessionId,
                key2 = reloadTrigger.value
            ) {
                value = try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        RemoteImageEntryPoint::class.java
                    )
                    entryPoint.supabaseStorageHelper().listSessionPhotos(sessionId)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selected photo index shown in the main RemoteImage (defaults to favorite)
            var selectedIndex by remember { mutableStateOf(0) }
            // Confirmation dialog state for photo deletion
            var showDeleteConfirm by remember { mutableStateOf(false) }

            // Compute favorite index to use as a default/restore point
            val favoriteIndex = remember(sessionPhotos) {
                sessionPhotos.indexOfFirst { url ->
                    val name = url.substringAfterLast('/')
                    name.startsWith(prefix = "fav_", ignoreCase = true)
                }
            }

            // Keep currently selected URL when the list reloads; otherwise fallback to favorite, then first
            LaunchedEffect(sessionPhotos) {
                val currentUrl = sessionPhotos.getOrNull(selectedIndex)
                val keepIndex = currentUrl?.let { sessionPhotos.indexOf(it) } ?: -1
                selectedIndex = when {
                    keepIndex >= 0 -> keepIndex
                    sessionPhotos.isNotEmpty() && favoriteIndex >= 0 -> favoriteIndex
                    sessionPhotos.isNotEmpty() -> 0
                    else -> 0
                }
            }

            // Main displayed image with overlay navigation and action buttons
            if (sessionPhotos.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Current image
                    RemoteImage(
                        url = sessionPhotos[selectedIndex],
                        // Reuse existing localized description
                        contentDescription = stringResource(id = R.string.session_carousel_image_content_description),
                        modifier = Modifier.fillMaxWidth(),
                        // Ensure the image takes the full available width while preserving aspect ratio
                        contentScale = ContentScale.FillWidth
                    )

                    // Left navigation button (no text, icon only)
                    IconButton(
                        onClick = {
                            val size = sessionPhotos.size
                            if (size > 0) {
                                selectedIndex = (selectedIndex - 1 + size) % size
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_chevron_left_24),
                            contentDescription = stringResource(id = R.string.session_carousel_prev_button)
                        )
                    }

                    // Right navigation button (no text, icon only)
                    IconButton(
                        onClick = {
                            val size = sessionPhotos.size
                            if (size > 0) {
                                selectedIndex = (selectedIndex + 1) % size
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_chevron_right_24),
                            contentDescription = stringResource(id = R.string.session_carousel_next_button)
                        )
                    }

                    // Floating action container (bottom-right) for favorite and delete
                    if (canUploadPhotos) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 4.dp)
                        ) {
                            val currentUrl = sessionPhotos.getOrNull(selectedIndex)
                            val isFavorite = currentUrl?.contains("/fav_", ignoreCase = false) == true
                            // Favorite toggle button (star icon only)
                            IconButton(onClick = {
                                val url = sessionPhotos.getOrNull(selectedIndex)
                                if (url != null) {
                                    val entryPoint = EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        RemoteImageEntryPoint::class.java
                                    )
                                    val storage = entryPoint.supabaseStorageHelper()
                                    coroutineScope.launch {
                                        val newFav = runCatching {
                                            storage.markSessionPhotoAsFavorite(sessionId, url)
                                        }.onFailure { t ->
                                            Log.w("PastSession", "Mark favorite failed for $url", t)
                                        }.getOrNull()
                                        if (newFav != null) {
                                            // Reload the photos to reflect the new favorite state
                                            reloadTrigger.value++
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = if (isFavorite) R.drawable.baseline_star_24 else R.drawable.baseline_star_border_24),
                                    contentDescription = stringResource(id = R.string.session_carousel_mark_favorite_description)
                                )
                            }
                            // Delete current photo
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_delete_24),
                                    contentDescription = stringResource(id = R.string.session_carousel_delete_photo_description)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Show the add-photos button only for the session owner (admin)
                if (canUploadPhotos) {
                    IconButton(onClick = { pickMultipleLauncher.launch("image/*") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_add_photo_alternate_24),
                            contentDescription = stringResource(id = R.string.past_session_upload_photos_description)
                        )
                    }
                }
                // Thumbnails with click to change the main displayed image
                sessionPhotos.forEachIndexed { index, url ->
                    RemoteImage(
                        url = url,
                        contentDescription = stringResource(id = R.string.past_session_photo_thumbnail_description),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(enabled = sessionPhotos.isNotEmpty()) {
                                selectedIndex = index
                            }
                    )
                }
            }

            // Deletion confirmation dialog (only shown when requested)
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text(text = stringResource(id = R.string.session_carousel_delete_confirm_title))
                    },
                    text = {
                        Text(text = stringResource(id = R.string.session_carousel_delete_confirm_message))
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val url = sessionPhotos.getOrNull(selectedIndex)
                            if (url != null) {
                                val entryPoint = EntryPointAccessors.fromApplication(
                                    context.applicationContext,
                                    RemoteImageEntryPoint::class.java
                                )
                                val storage = entryPoint.supabaseStorageHelper()
                                coroutineScope.launch {
                                    val ok = runCatching { storage.deleteSessionPhotoByUrl(url) }
                                        .onFailure { t ->
                                            Log.w(
                                                "PastSession",
                                                "Delete failed for $url",
                                                t
                                            )
                                        }
                                        .getOrElse { false }

                                    if (ok) {
                                        // Adjust selection and refresh the list
                                        val newCount = (sessionPhotos.size - 1).coerceAtLeast(0)
                                        if (newCount <= 0) {
                                            selectedIndex = 0
                                        } else if (selectedIndex >= newCount) {
                                            selectedIndex = newCount - 1
                                        }
                                        reloadTrigger.value++
                                    }
                                    showDeleteConfirm = false
                                }
                            } else {
                                showDeleteConfirm = false
                            }
                        }) {
                            Text(text = stringResource(id = R.string.session_carousel_delete_confirm_delete_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(text = stringResource(id = R.string.session_carousel_delete_confirm_cancel_button))
                        }
                    }
                )
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
                    text = R.string.ongoing_session_label_holes_played.let { resId ->
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
