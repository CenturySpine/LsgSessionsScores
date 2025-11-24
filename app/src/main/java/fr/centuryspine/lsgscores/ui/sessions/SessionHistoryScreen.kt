package fr.centuryspine.lsgscores.ui.sessions

import ExportHelpers
import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.ui.common.usePhotoCameraLauncher
import fr.centuryspine.lsgscores.ui.common.usePhotoGalleryLauncher
import fr.centuryspine.lsgscores.ui.components.WeatherSummaryRow
import fr.centuryspine.lsgscores.utils.SessionFormatters
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    navController: androidx.navigation.NavController,
    sessionViewModel: SessionViewModel
) {
    val completedSessions by sessionViewModel.completedSessions.collectAsStateWithLifecycle()
    val scoringModes by sessionViewModel.scoringModes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Inject GameZoneViewModel for fetching zone names
    val gameZoneViewModel: fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel = hiltViewModel()
    // Current authenticated user id used to decide if actions are visible
    val currentUserId = remember { sessionViewModel.currentUserIdOrNull() }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<Session?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission handled in generateAndSharePdf - weather will be null if denied
    }

    Scaffold(
        topBar = {

        }
    ) { paddingValues ->
        if (completedSessions.isEmpty()) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(completedSessions, key = { it.id }) { session ->
                    // Compute localized scoring mode label to display next to date/time using cached list
                    val scoringModeLabel = scoringModes
                        .firstOrNull { it.id == session.scoringModeId }
                        ?.getLocalizedName(context)
                    // Load game zone name from its id (suspend DAO available)
                    val gameZoneName by produceState<String?>(
                        initialValue = null,
                        key1 = session.gameZoneId
                    ) {
                        value = try {
                            gameZoneViewModel.getGameZoneById(session.gameZoneId)?.name
                        } catch (_: Exception) {
                            null
                        }
                    }
                    SessionHistoryCard(
                        session = session,
                        scoringModeLabel = scoringModeLabel,
                        gameZoneLabel = gameZoneName,
                        canManageSession = (session.userId == currentUserId),
                        onCardClick = { selectedSession ->
                            // Navigate to the new read-only past session details screen
                            // Important: use actual string interpolation so the route matches the graph
                            navController.navigate("past_session_detail/${selectedSession.id}")
                        },
                        onExportClick = { selectedSession ->
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            ExportHelpers.generateAndSharePdf(context, selectedSession, sessionViewModel)
                        },
                        onExportPhoto = { _, photoPath ->
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            ExportHelpers.generateAndShareImageExport(
                                context,
                                session,
                                sessionViewModel,
                                photoPath
                            )
                        },
                        onDeleteClick = { selectedSession ->
                            sessionToDelete = selectedSession
                            showDeleteDialog = true
                        },
                        onEditClick = { selectedSession ->
                            sessionToEdit = selectedSession
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                sessionToDelete = null
            },
            title = { Text(stringResource(R.string.session_history_delete_dialog_title)) },
            text = { Text(stringResource(R.string.session_history_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.let { session ->
                            sessionViewModel.deleteSessionAndAllData(session)
                        }
                        showDeleteDialog = false
                        sessionToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.session_history_delete_dialog_button_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        sessionToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.session_history_delete_dialog_button_cancel))
                }
            }
        )
    }

    // Edit Session dialog
    sessionToEdit?.let { editing ->
        val context = LocalContext.current
        var errorText by remember { mutableStateOf<String?>(null) }

        // States for date and times
        var selectedDate by remember { mutableStateOf(editing.dateTime.toLocalDate()) }
        var selectedStartTime by remember { mutableStateOf(editing.dateTime.toLocalTime()) }
        var selectedEndTime by remember { mutableStateOf(editing.endDateTime?.toLocalTime()) }

        val dateFormatterUi = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
        val timeFormatterUi = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        // Helpers to format
        fun formatDate() = selectedDate.format(dateFormatterUi)
        fun formatStartTime() = selectedStartTime.format(timeFormatterUi)
        fun formatEndTime() = selectedEndTime?.format(timeFormatterUi) ?: ""

        AlertDialog(
            onDismissRequest = { sessionToEdit = null },
            title = { Text(stringResource(R.string.edit_session_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Date field (opens a DatePicker)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDate(),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.edit_session_date_label)) },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable {
                                    val y = selectedDate.year
                                    val m = selectedDate.monthValue - 1
                                    val d = selectedDate.dayOfMonth
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
                                            // Disallow future date
                                            if (picked.isAfter(LocalDate.now())) {
                                                errorText = context.getString(R.string.edit_session_error_future_start)
                                            } else {
                                                errorText = null
                                                selectedDate = picked
                                                // If end exists and is before start after date change, clear end
                                                selectedEndTime?.let {
                                                    val tentativeEnd = LocalDateTime.of(picked, it)
                                                    val tentativeStart = LocalDateTime.of(picked, selectedStartTime)
                                                    if (tentativeEnd.isBefore(tentativeStart)) {
                                                        selectedEndTime = null
                                                    }
                                                }
                                            }
                                        },
                                        y, m, d
                                    ).apply {
                                        datePicker.maxDate = System.currentTimeMillis()
                                    }.show()
                                }
                        )
                    }

                    // Start time field
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatStartTime(),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.edit_session_start_time_label)) },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    val hour = selectedStartTime.hour
                                    val minute = selectedStartTime.minute
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            errorText = null
                                            selectedStartTime = LocalTime.of(h, min)
                                        },
                                        hour, minute, true
                                    ).show()
                                }
                        )
                    }

                    // End time field (optional)
                    OutlinedTextField(
                        value = formatEndTime(),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.edit_session_end_time_label)) },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        leadingIcon = {
                            IconButton(onClick = {
                                val hour = selectedEndTime?.hour ?: selectedStartTime.hour
                                val minute = selectedEndTime?.minute ?: selectedStartTime.minute
                                TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        errorText = null
                                        selectedEndTime = LocalTime.of(h, min)
                                    },
                                    hour, minute, true
                                ).show()
                            }) {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { selectedEndTime = null }) {
                                Text(text = stringResource(R.string.edit_session_clear_end_time))
                            }
                        }
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val newStart = LocalDateTime.of(selectedDate, selectedStartTime)
                        val newEnd = selectedEndTime?.let { LocalDateTime.of(selectedDate, it) }

                        val now = LocalDateTime.now()
                        if (newStart.isAfter(now)) {
                            errorText = context.getString(R.string.edit_session_error_future_start)
                            return@TextButton
                        }
                        if (newEnd != null) {
                            if (newEnd.isAfter(now)) {
                                errorText = context.getString(R.string.edit_session_error_future_end)
                                return@TextButton
                            }
                            if (newEnd.isBefore(newStart)) {
                                errorText = context.getString(R.string.edit_session_error_end_before_start)
                                return@TextButton
                            }
                        }

                        sessionViewModel.updateSessionDateTimes(editing.id, newStart, newEnd) { ok, code ->
                            if (ok) {
                                sessionToEdit = null
                                errorText = null
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.player_detail_button_save),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                errorText = when (code) {
                                    "future_start" -> context.getString(R.string.edit_session_error_future_start)
                                    "future_end" -> context.getString(R.string.edit_session_error_future_end)
                                    "end_before_start" -> context.getString(R.string.edit_session_error_end_before_start)
                                    else -> code ?: "Unknown error"
                                }
                            }
                        }
                    } catch (_: Exception) {
                        errorText = context.getString(R.string.edit_session_error_invalid_format)
                    }
                }) {
                    Text(stringResource(R.string.edit_session_button_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToEdit = null }) {
                    Text(stringResource(R.string.edit_session_button_cancel))
                }
            }
        )
    }
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    scoringModeLabel: String? = null,
    gameZoneLabel: String? = null,
    canManageSession: Boolean,
    onCardClick: (Session) -> Unit,
    onExportClick: (Session) -> Unit,
    onExportPhoto: (Session, String) -> Unit,
    onDeleteClick: (Session) -> Unit,
    onEditClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(session) },
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
            // First line: Date - Start time (no "at" label), full width
            val currentLocale = Locale.getDefault()
            val datePattern = stringResource(R.string.session_history_date_format_pattern)
            val formattedDate = session.dateTime.format(
                DateTimeFormatter.ofPattern(datePattern, currentLocale)
            )
            val formattedTime = session.dateTime.format(
                DateTimeFormatter.ofPattern("HH:mm", currentLocale)
            )
            val displayDate = if (currentLocale.language == "fr") {
                formattedDate.replaceFirstChar { it.uppercase() }
            } else {
                formattedDate
            }
            // Header contains only date and time
            val headerText = "$displayDate - $formattedTime"
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium
            )

            // Second line: game zone name and scoring mode label
            val secondaryLine = listOfNotNull(
                gameZoneLabel?.takeIf { it.isNotBlank() },
                scoringModeLabel?.takeIf { it.isNotBlank() }
            ).joinToString(" - ")
            if (secondaryLine.isNotBlank()) {
                Text(
                    text = secondaryLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Third line: duration (value only) + weather + actions (share/edit/delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration (only the value, no label), shown if end time exists
                session.endDateTime?.let { endTime ->
                    val durationText = SessionFormatters.formatSessionDuration(context, session.dateTime, endTime)
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Weather info (icon + temperature + wind)
                session.weatherData?.let { weather ->
                    Spacer(modifier = Modifier.width(12.dp))
                    WeatherSummaryRow(weatherInfo = weather, iconSize = 32.dp)
                }

                Spacer(modifier = Modifier.weight(1f))

                if (canManageSession) {
                    // Export menu (Share) - visible only to the session owner
                    var showExportMenu by remember { mutableStateOf(false) }
                    val launchCamera = usePhotoCameraLauncher { photoPath ->
                        photoPath?.let { onExportPhoto(session, it) }
                    }
                    val launchGallery = usePhotoGalleryLauncher { photoPath ->
                        photoPath?.let { onExportPhoto(session, it) }
                    }

                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.session_history_export_menu_description)
                            )
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            // Camera export option
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.session_history_export_camera))
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    launchCamera()
                                }
                            )

                            // Gallery export option
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.baseline_add_photo_alternate_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.session_history_export_gallery))
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    launchGallery()
                                }
                            )

                            HorizontalDivider()

                            // PDF export option
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.session_history_export_pdf))
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    onExportClick(session)
                                }
                            )
                        }
                    }

                    // Edit button - visible only to the session owner
                    IconButton(onClick = { onEditClick(session) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.session_history_edit_icon_description)
                        )
                    }

                    // Delete button - visible only to the session owner
                    IconButton(onClick = { onDeleteClick(session) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.session_history_delete_icon_description),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}





