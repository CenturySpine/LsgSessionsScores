package fr.centuryspine.lsgscores.ui.sessions

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.ui.common.usePhotoCameraLauncher
import fr.centuryspine.lsgscores.ui.common.usePhotoGalleryLauncher
import fr.centuryspine.lsgscores.ui.components.WeatherIcon
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessionViewModel: SessionViewModel
) {
    val completedSessions by sessionViewModel.completedSessions.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                    SessionHistoryCard(
                        session = session,
                        onExportClick = { selectedSession ->
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            generateAndSharePdf(context, selectedSession, sessionViewModel)
                        },
                        onExportPhoto = { _, photoPath ->
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            generateAndShareImageExport(
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
    onExportClick: (Session) -> Unit,
    onExportPhoto: (Session, String) -> Unit,
    onDeleteClick: (Session) -> Unit,
    onEditClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    session.endDateTime?.let { endTime ->
                        val duration = Duration.between(session.dateTime, endTime)
                        val hours = duration.toHours()
                        val minutes = duration.toMinutes() % 60
                        val durationText = when {
                            hours > 0 -> if (minutes > 0) stringResource(
                                R.string.session_history_duration_hours_minutes,
                                hours,
                                minutes
                            )
                            else stringResource(R.string.session_history_duration_hours, hours)

                            else -> stringResource(
                                R.string.session_history_duration_minutes,
                                minutes
                            )
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
            session.weatherData?.let { weather ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherIcon(
                        weatherInfo = weather,
                        size = 32.dp
                    )
                    Column {
                        Text(
                            text = "${weather.temperature}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${weather.windSpeedKmh} km/h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Export menu
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

            // Edit button
            IconButton(onClick = { onEditClick(session) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.session_history_edit_icon_description)
                )
            }

            // Delete button
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


private fun generateAndSharePdf(
    context: Context,
    session: Session,
    sessionViewModel: SessionViewModel
) {
    sessionViewModel.viewModelScope.launch {
        var pdfDocument: PdfDocument? = null
        try {
            Toast.makeText(
                context,
                context.getString(R.string.exporting_pdf_toast_message),
                Toast.LENGTH_SHORT
            ).show()
            val pdfData = sessionViewModel.loadSessionPdfData(session).first()
            val weatherInfo = session.weatherData ?: sessionViewModel.getCurrentWeatherInfo()

            pdfDocument = PdfDocument()

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val defaultTextSize = 10f

            val paint = Paint().apply {
                textSize = defaultTextSize
            }
            val boldPaint = Paint().apply {
                isFakeBoldText = true
                textSize = defaultTextSize
            }
            val italicPaint = Paint().apply {
                isFakeBoldText = false
                textSkewX = -0.25f
                textSize = defaultTextSize
            }
            val gameModePaint = Paint().apply {
                textSize = 8f
                color = Color.GRAY
            }

            var yPosition = 40f
            val xMargin = 20f
            val lineSpacing = 18f
            val cellPadding = 5f


            // Calculate ascent/descent for vertical centering AFTER setting textSize
            val textCenterOffsetYPaint = (paint.ascent() + paint.descent()) / 2f
            val textCenterOffsetYBoldPaint = (boldPaint.ascent() + boldPaint.descent()) / 2f
            val textCenterOffsetYItalicPaint = (italicPaint.ascent() + italicPaint.descent()) / 2f


            yPosition += lineSpacing / 2f


            // Session Name
            paint.textSize = 16f // Temporarily increase for session name
            boldPaint.textSize = 16f
            val sessionNameLabel = "${context.getString(R.string.pdf_session_name_prefix)} "
            val sessionNameLabelWidth = boldPaint.measureText(sessionNameLabel)
            val sessionNameTextCenterOffsetYLarge =
                (boldPaint.ascent() + boldPaint.descent()) / 2f // For 16f

            canvas.drawText(
                sessionNameLabel,
                xMargin,
                yPosition - sessionNameTextCenterOffsetYLarge,
                boldPaint
            )
            canvas.drawText(
                pdfData.gameZone?.name ?: "N/A",
                xMargin + sessionNameLabelWidth,
                yPosition - sessionNameTextCenterOffsetYLarge,
                paint
            )

            paint.textSize = defaultTextSize // Reset to default
            boldPaint.textSize = defaultTextSize
            yPosition += lineSpacing * 2f


            // Basic Session Info
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

            // Calculate table width first (moved up from later in function)
            val numHoles = pdfData.playedHoles.size
            val availableWidthForTable = pageInfo.pageWidth - (2 * xMargin)

            // Two columns layout
            val leftColumnX = xMargin
            val rightColumnX = xMargin + (availableWidthForTable * 0.6f) // 60% for left column
            var currentY = yPosition

            // LEFT COLUMN - Session Info
            var labelText = "${context.getString(R.string.pdf_label_session_date)} "
            var labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(
                labelText,
                leftColumnX,
                currentY - textCenterOffsetYBoldPaint,
                boldPaint
            )
            canvas.drawText(
                pdfData.session.dateTime.format(dateFormatter),
                leftColumnX + labelWidth,
                currentY - textCenterOffsetYPaint,
                paint
            )
            currentY += lineSpacing

            // Session Start Time
            labelText = "${context.getString(R.string.pdf_label_session_start_time)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(
                labelText,
                leftColumnX,
                currentY - textCenterOffsetYBoldPaint,
                boldPaint
            )
            canvas.drawText(
                pdfData.session.dateTime.format(timeFormatter),
                leftColumnX + labelWidth,
                currentY - textCenterOffsetYPaint,
                paint
            )
            currentY += lineSpacing

            // Session End Time
            labelText = "${context.getString(R.string.pdf_label_session_end_time)} "
            labelWidth = boldPaint.measureText(labelText)
            val endTimeValue = pdfData.session.endDateTime?.format(timeFormatter)
                ?: context.getString(R.string.pdf_not_applicable)
            canvas.drawText(
                labelText,
                leftColumnX,
                currentY - textCenterOffsetYBoldPaint,
                boldPaint
            )
            canvas.drawText(
                endTimeValue,
                leftColumnX + labelWidth,
                currentY - textCenterOffsetYPaint,
                paint
            )
            currentY += lineSpacing

            // Session Type
            labelText = "${context.getString(R.string.pdf_session_type_prefix)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(
                labelText,
                leftColumnX,
                currentY - textCenterOffsetYBoldPaint,
                boldPaint
            )
            canvas.drawText(
                pdfData.session.sessionType.toString(),
                leftColumnX + labelWidth,
                currentY - textCenterOffsetYPaint,
                paint
            )
            currentY += lineSpacing

            // Scoring Mode
            val scoringMode = try {
                sessionViewModel.scoringModes.first().find { it.id == pdfData.session.scoringModeId }
            } catch (_: Exception) {
                null
            }
            labelText = "${context.getString(R.string.pdf_scoring_mode_prefix)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(
                labelText,
                leftColumnX,
                currentY - textCenterOffsetYBoldPaint,
                boldPaint
            )
            canvas.drawText(
                scoringMode?.getLocalizedName(context) ?: "Unknown",
                leftColumnX + labelWidth,
                currentY - textCenterOffsetYPaint,
                paint
            )
            currentY += lineSpacing

            // RIGHT COLUMN - Weather Info
            if (weatherInfo != null) {
                var weatherY = yPosition // Start from same Y as left column

                // Weather title
                val weatherTitle = context.getString(R.string.pdf_weather_title)
                canvas.drawText(
                    weatherTitle,
                    rightColumnX,
                    weatherY - textCenterOffsetYBoldPaint,
                    boldPaint
                )
                weatherY += lineSpacing

                // Temperature
                val tempText = "${weatherInfo.temperature}°C"
                canvas.drawText(tempText, rightColumnX, weatherY - textCenterOffsetYPaint, paint)
                weatherY += lineSpacing

                // Weather description
                canvas.drawText(
                    weatherInfo.description,
                    rightColumnX,
                    weatherY - textCenterOffsetYPaint,
                    italicPaint
                )
                weatherY += lineSpacing

                // Wind
                val windText =
                    "${context.getString(R.string.pdf_wind_label)} ${weatherInfo.windSpeedKmh} km/h"
                canvas.drawText(windText, rightColumnX, weatherY - textCenterOffsetYPaint, paint)
            }

            // Update yPosition to after both columns
            yPosition = currentY

            // Session Comment (full width, below both columns)
            pdfData.session.comment?.takeIf { it.isNotBlank() }?.let {
                labelText = "${context.getString(R.string.pdf_comment_prefix)} "
                labelWidth = boldPaint.measureText(labelText)
                canvas.drawText(
                    labelText,
                    xMargin,
                    yPosition - textCenterOffsetYBoldPaint,
                    boldPaint
                )
                canvas.drawText(it, xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint)
                yPosition += lineSpacing
            }
            yPosition += lineSpacing // Extra space before table


            // Scores Table

            val teamNameColWidth = availableWidthForTable * 0.22f
            val totalColWidth = availableWidthForTable * 0.13f
            val scoreColWidth =
                (availableWidthForTable - teamNameColWidth - totalColWidth) / numHoles.coerceAtLeast(
                    1
                )

            // Helper to wrap text within a max width
            fun wrapText(text: String, p: Paint, maxWidth: Float): List<String> {
                if (text.isBlank()) return listOf("")
                val words = text.trim().split(" ")
                val lines = mutableListOf<String>()
                var current = StringBuilder()
                for (word in words) {
                    val candidate = if (current.isEmpty()) word else current.toString() + " " + word
                    if (p.measureText(candidate) <= maxWidth) {
                        if (current.isEmpty()) current.append(word) else { current.append(" "); current.append(word) }
                    } else {
                        if (current.isNotEmpty()) {
                            lines.add(current.toString())
                            current = StringBuilder(word)
                        } else {
                            // Single very long word: break by characters
                            var start = 0
                            while (start < word.length) {
                                var end = word.length
                                var found = false
                                while (end > start) {
                                    if (p.measureText(word.substring(start, end)) <= maxWidth) {
                                        lines.add(word.substring(start, end))
                                        start = end
                                        found = true
                                        break
                                    }
                                    end--
                                }
                                if (!found) {
                                    // Fallback to ensure progress
                                    val next = (start + 1).coerceAtMost(word.length)
                                    lines.add(word.substring(start, next))
                                    start = next
                                }
                            }
                            current = StringBuilder()
                        }
                    }
                }
                if (current.isNotEmpty()) lines.add(current.toString())
                return lines
            }

            // Pre-compute wrapped hole name lines to know header height
            val headerCellInnerWidth = scoreColWidth - 2 * cellPadding
            data class HeaderCol(val wrappedNameLines: List<String>, val gameMode: String)
            val headerCols = pdfData.playedHoles.map { playedHole ->
                val holeDetail = pdfData.holesDetails[playedHole.holeId]
                val holeName = holeDetail?.name?.takeIf { it.isNotBlank() } ?: "${context.getString(R.string.pdf_hole_prefix)} ${playedHole.position}"
                val gameModeName = pdfData.holeGameModes[playedHole.gameModeId.toLong()] ?: ""
                HeaderCol(wrapText(holeName, boldPaint, headerCellInnerWidth), gameModeName)
            }
            val maxNameLines = headerCols.maxOfOrNull { it.wrappedNameLines.size } ?: 1

            // Compute dynamic header height considering both hole name lines + game mode and wrapped left header
            val teamHeaderInnerWidth = teamNameColWidth - 2 * cellPadding
            val teamHeaderLines = wrapText(context.getString(R.string.pdf_header_team_players), boldPaint, teamHeaderInnerWidth)
            val tableHeaderHeight = lineSpacing * maxOf(maxNameLines + 1.2f, teamHeaderLines.size.toFloat())

            val tableTopY = yPosition + 4f

            val headerCenterY = tableTopY + tableHeaderHeight / 2f
            val textCenterOffsetYGameModePaint = (gameModePaint.ascent() + gameModePaint.descent()) / 2f

            // Table Headers
            var currentX = xMargin
            // Leftmost header: Team / Players (wrapped like hole names)
            teamHeaderLines.forEachIndexed { idx, line ->
                val baseY = tableTopY + lineSpacing * (idx + 1) - textCenterOffsetYBoldPaint
                canvas.drawText(
                    line,
                    currentX + cellPadding,
                    baseY,
                    boldPaint
                )
            }
            currentX += teamNameColWidth

            // Hole columns: draw wrapped hole name lines, then game mode
            headerCols.forEach { col ->
                // Draw each wrapped line, centered horizontally
                col.wrappedNameLines.forEachIndexed { idx, line ->
                    val w = boldPaint.measureText(line)
                    val baseY = tableTopY + lineSpacing * (idx + 1) - textCenterOffsetYBoldPaint
                    canvas.drawText(line, currentX + (scoreColWidth - w) / 2f, baseY, boldPaint)
                }
                // Draw game mode below the name block
                val gameModeName = col.gameMode
                if (gameModeName.isNotBlank()) {
                    val w2 = gameModePaint.measureText(gameModeName)
                    val baseY2 = tableTopY + lineSpacing * (maxNameLines + 0.9f) - textCenterOffsetYGameModePaint
                    canvas.drawText(gameModeName, currentX + (scoreColWidth - w2) / 2f, baseY2, gameModePaint)
                }
                currentX += scoreColWidth
            }

            // Total header
            canvas.drawText(
                context.getString(R.string.pdf_header_total),
                currentX + (totalColWidth - boldPaint.measureText(context.getString(R.string.pdf_header_total))) / 2,
                headerCenterY - textCenterOffsetYBoldPaint,
                boldPaint
            )

            yPosition = tableTopY + tableHeaderHeight
            // Draw header bottom line (top border of first row)
            canvas.drawLine(
                xMargin,
                yPosition,
                xMargin + availableWidthForTable,
                yPosition,
                paint
            )

            // Table Rows
            pdfData.teams.forEach { teamData ->
                currentX = xMargin
                val teamDisplayText = "${teamData.position}. ${teamData.teamName}"
                val teamCellInnerWidth = teamNameColWidth - 2 * cellPadding
                val teamLines = wrapText(teamDisplayText, paint, teamCellInnerWidth)
                val rowHeight = (teamLines.size.coerceAtLeast(1)) * lineSpacing
                val rowCenterY = yPosition + rowHeight / 2f
                // Draw team cell as multi-line, vertically centered like scores
                teamLines.forEachIndexed { idx, line ->
                    val lineCenterY = rowCenterY + (idx - (teamLines.size - 1) / 2f) * lineSpacing
                    val baseY = lineCenterY - textCenterOffsetYPaint
                    canvas.drawText(
                        line,
                        currentX + cellPadding,
                        baseY,
                        paint
                    )
                }
                currentX += teamNameColWidth

                // Draw scores for each hole
                pdfData.playedHoles.forEach { playedHole ->
                    val scoreData = teamData.holeScores[playedHole.id]

                    if (scoreData != null) {
                        val scoreString = scoreData.calculatedScore.toString()
                        val strokesString = "(${scoreData.strokes})"
                        val separator = " - "

                        val scoreWidth = boldPaint.measureText(scoreString)
                        val separatorWidth = paint.measureText(separator)
                        val strokesWidth = italicPaint.measureText(strokesString)
                        val totalTextWidth = scoreWidth + separatorWidth + strokesWidth

                        var textX = currentX + (scoreColWidth - totalTextWidth) / 2
                        canvas.drawText(
                            scoreString,
                            textX,
                            rowCenterY - textCenterOffsetYBoldPaint,
                            boldPaint
                        )
                        textX += scoreWidth
                        canvas.drawText(separator, textX, rowCenterY - textCenterOffsetYPaint, paint)
                        textX += separatorWidth
                        canvas.drawText(
                            strokesString,
                            textX,
                            rowCenterY - textCenterOffsetYItalicPaint,
                            italicPaint
                        )
                    } else {
                        val scoreText = "-"
                        val textWidth = paint.measureText(scoreText)
                        canvas.drawText(
                            scoreText,
                            currentX + (scoreColWidth - textWidth) / 2,
                            rowCenterY - textCenterOffsetYPaint,
                            paint
                        )
                    }
                    currentX += scoreColWidth
                }

                // Draw total
                val totalScoreString = teamData.totalCalculatedScore.toString()
                val totalStrokesString = "(${teamData.totalStrokes})"
                val separator = " - "

                val totalScoreWidth = boldPaint.measureText(totalScoreString)
                val separatorWidth = paint.measureText(separator)
                val totalStrokesWidth = italicPaint.measureText(totalStrokesString)
                val totalTextWidth = totalScoreWidth + separatorWidth + totalStrokesWidth

                var textX = currentX + (totalColWidth - totalTextWidth) / 2
                canvas.drawText(
                    totalScoreString,
                    textX,
                    rowCenterY - textCenterOffsetYBoldPaint,
                    boldPaint
                )
                textX += totalScoreWidth
                canvas.drawText(separator, textX, rowCenterY - textCenterOffsetYPaint, paint)
                textX += separatorWidth
                canvas.drawText(
                    totalStrokesString,
                    textX,
                    rowCenterY - textCenterOffsetYItalicPaint,
                    italicPaint
                )

                canvas.drawLine(
                    xMargin,
                    yPosition + rowHeight,
                    xMargin + availableWidthForTable,
                    yPosition + rowHeight,
                    paint
                )
                yPosition += rowHeight
            }

            val tableBottomY = yPosition
            var lineX = xMargin + teamNameColWidth
            canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint)
            (0 until numHoles).forEach { _ ->
                // Draw lines between score columns
                lineX += scoreColWidth
                canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint)
            }
            // Draw table borders
            canvas.drawLine(
                xMargin,
                tableTopY,
                xMargin + availableWidthForTable,
                tableTopY,
                paint
            ) // Top border
            canvas.drawLine(xMargin, tableTopY, xMargin, tableBottomY, paint) // Left border
            canvas.drawLine(
                xMargin + availableWidthForTable,
                tableTopY,
                xMargin + availableWidthForTable,
                tableBottomY,
                paint
            ) // Right border
            // Bottom border is already drawn by the last row's line

            // Footer signature
            yPosition += lineSpacing / 2f // Half line spacing
            val footerText = context.getString(R.string.pdf_footer_created_by)
            val footerPaint = Paint().apply {
                textSize = defaultTextSize
                textSkewX = -0.25f // Italic
                color = "#555555".toColorInt() // Dark gray
            }
            val footerTextWidth = footerPaint.measureText(footerText)
            val footerX = xMargin + availableWidthForTable - footerTextWidth
            val footerTextCenterOffsetY = (footerPaint.ascent() + footerPaint.descent()) / 2f
            canvas.drawText(footerText, footerX, yPosition - footerTextCenterOffsetY, footerPaint)

            pdfDocument.finishPage(page)

            val timeStamp = SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(System.currentTimeMillis())
            val fileName = "session_${pdfData.session.id}_${timeStamp}.pdf"
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val pdfFile = File(pdfDir, fileName)

            FileOutputStream(pdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }

            val pdfUri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.share_session_pdf_title)
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "${context.getString(R.string.pdf_generation_failed_toast_message)} ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            pdfDocument?.close()
        }
    }

}

private fun generateAndShareImageExport(
    context: Context,
    session: Session,
    sessionViewModel: SessionViewModel,
    imagePath: String
) {
    sessionViewModel.viewModelScope.launch {
        try {
            Toast.makeText(context, "Generating image export...", Toast.LENGTH_SHORT).show()

            // Get session data and weather info
            val pdfData = sessionViewModel.loadSessionPdfData(session).first()
            val weatherInfo = session.weatherData ?: sessionViewModel.getCurrentWeatherInfo()

            // Load the base image
            val originalBitmap = BitmapFactory.decodeFile(imagePath)
            if (originalBitmap == null) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Create a mutable copy for drawing
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            // Set up paint styles with bigger fonts
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 240f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }

            val smallTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 100f
                isAntiAlias = true
                typeface = Typeface.DEFAULT
            }

            val footerPaint = Paint().apply {
                color = Color.WHITE
                textSize = 80f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                alpha = 180 // Semi-transparent
            }

            val margin = 40f
            val lineHeight = 180f

            // TOP LEFT - Game Zone + Date
            var yPos = margin + textPaint.textSize
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

            canvas.drawText(pdfData.gameZone?.name ?: "Unknown Zone", margin, yPos, textPaint)
            yPos += lineHeight
            canvas.drawText(
                pdfData.session.dateTime.format(dateFormatter),
                margin,
                yPos,
                smallTextPaint
            )

            // TOP RIGHT - Weather Info
            if (weatherInfo != null) {
                val rightMargin = mutableBitmap.width - margin
                var weatherY = margin + textPaint.textSize

                val tempText = "${weatherInfo.temperature}°C"
                val tempWidth = textPaint.measureText(tempText)
                canvas.drawText(tempText, rightMargin - tempWidth, weatherY, textPaint)

                weatherY += lineHeight
                val windText = "${weatherInfo.windSpeedKmh} km/h"
                val windWidth = smallTextPaint.measureText(windText)
                canvas.drawText(windText, rightMargin - windWidth, weatherY, smallTextPaint)

                weatherY += lineHeight
                val descWidth = smallTextPaint.measureText(weatherInfo.description)
                canvas.drawText(
                    weatherInfo.description,
                    rightMargin - descWidth,
                    weatherY,
                    smallTextPaint
                )
            }

            // BOTTOM LEFT - Results Summary (start from bottom and go up)
            val bottomMargin = mutableBitmap.height - margin
            var resultsY = bottomMargin -  40f // Leave space for footer

// Calculate max width of team names for proper alignment
            val maxNameWidth = pdfData.teams.maxOfOrNull { teamData ->
                val nameText = "${teamData.position}. ${teamData.teamName}"
                smallTextPaint.measureText(nameText)
            } ?: 0f

            val scoreStartX = margin + maxNameWidth + 60f // 60f padding between name and score

// Draw teams from last to first (bottom to top)
            pdfData.teams.asReversed().forEach { teamData ->
                val resultText = "${teamData.position}. ${teamData.teamName}"
                canvas.drawText(resultText, margin, resultsY, smallTextPaint)

                val scoreText = "${teamData.totalCalculatedScore} - ${teamData.totalStrokes}"
                canvas.drawText(scoreText, scoreStartX, resultsY, smallTextPaint)

                resultsY -= lineHeight // Go UP instead of down
            }

            // BOTTOM RIGHT - Footer
            val footerText = "Generated by LsgScores App"
            val footerWidth = footerPaint.measureText(footerText)
            canvas.drawText(
                footerText,
                mutableBitmap.width - footerWidth - margin,
                bottomMargin,
                footerPaint
            )

            // Save and share the image
            saveAndShareImage(context, mutableBitmap, session)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to generate image: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }
}

private fun saveAndShareImage(
    context: Context,
    bitmap: Bitmap,
    session: Session
) {
    try {
        // Generate filename
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val fileName = "session_${session.id}_${timeStamp}.jpg"

        // Save to gallery using MediaStore (Android 10+ compatible)
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LsgScores")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val imageUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (imageUri != null) {
            // Write bitmap to the URI
            context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            // Mark as not pending (Android 10+)
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(imageUri, contentValues, null, null)

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share session image"))
            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

