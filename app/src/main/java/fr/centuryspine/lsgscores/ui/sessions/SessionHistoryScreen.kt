package fr.centuryspine.lsgscores.ui.sessions

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.ui.common.CombinedPhotoPicker
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.painterResource
import fr.centuryspine.lsgscores.ui.common.GalleryPhotoPicker
import fr.centuryspine.lsgscores.ui.common.PhotoPicker
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import fr.centuryspine.lsgscores.ui.common.usePhotoCameraLauncher
import fr.centuryspine.lsgscores.ui.common.usePhotoGalleryLauncher
import fr.centuryspine.lsgscores.ui.components.WeatherIcon
import fr.centuryspine.lsgscores.viewmodel.CityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessionViewModel: SessionViewModel,
    cityViewModel: CityViewModel
) {
    val completedSessions by sessionViewModel.completedSessions.collectAsState()
    val context = LocalContext.current
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val selectedCityId by cityViewModel.selectedCityId.collectAsState()
    
    LaunchedEffect(selectedCityId) {
        sessionViewModel.updateSelectedCity(selectedCityId)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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
                items(completedSessions) { session ->
                    SessionHistoryCard(
                        session = session,
                        onExportClick = { selectedSession ->
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            generateAndSharePdf(context, selectedSession, sessionViewModel)

                        },
                        onExportPhoto = { selectedSession, photoPath ->
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
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    onExportClick: (Session) -> Unit,
    onExportPhoto: (Session, String) -> Unit,
    onDeleteClick: (Session) -> Unit,
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

            val teamNameColWidth = availableWidthForTable * 0.25f
            val totalColWidth = availableWidthForTable * 0.15f
            val scoreColWidth =
                (availableWidthForTable - teamNameColWidth - totalColWidth) / numHoles.coerceAtLeast(
                    1
                )

            val tableHeaderHeight = lineSpacing * 1.5f // Increased height for two lines

            val tableTopY = yPosition - tableHeaderHeight / 2f + 4f


            // Table Headers
            var currentX = xMargin
            val headerCenterY = tableTopY + tableHeaderHeight / 2f
            canvas.drawText(
                context.getString(R.string.pdf_header_team_players),
                currentX + cellPadding,
                headerCenterY - textCenterOffsetYBoldPaint,
                boldPaint
            )
            currentX += teamNameColWidth

            pdfData.playedHoles.forEach { playedHole ->
                val holeDetail = pdfData.holesDetails[playedHole.holeId]
                val holeName = holeDetail?.name?.takeIf { it.isNotBlank() } ?: "${
                    context.getString(
                        R.string.pdf_hole_prefix
                    )
                } ${playedHole.position}"
                val gameModeName = pdfData.holeGameModes[playedHole.gameModeId.toLong()] ?: ""

                val holeNameWidth = boldPaint.measureText(holeName)
                val gameModeNameWidth = gameModePaint.measureText(gameModeName)

                // Draw Hole Name (top part of the cell)
                canvas.drawText(
                    holeName,
                    currentX + (scoreColWidth - holeNameWidth) / 2,
                    headerCenterY - (lineSpacing / 4) - textCenterOffsetYBoldPaint,
                    boldPaint
                )
                // Draw Game Mode Name (bottom part of the cell)
                canvas.drawText(
                    gameModeName,
                    currentX + (scoreColWidth - gameModeNameWidth) / 2,
                    headerCenterY + (lineSpacing / 2) - textCenterOffsetYBoldPaint,
                    gameModePaint
                )

                currentX += scoreColWidth
            }
            canvas.drawText(
                context.getString(R.string.pdf_header_total),
                currentX + (totalColWidth - boldPaint.measureText(context.getString(R.string.pdf_header_total))) / 2,
                headerCenterY - textCenterOffsetYBoldPaint,
                boldPaint
            )

            yPosition = tableTopY + tableHeaderHeight - 4f
            canvas.drawLine(
                xMargin,
                yPosition + lineSpacing / 2f,
                xMargin + availableWidthForTable,
                yPosition + lineSpacing / 2f,
                paint
            )
            yPosition += lineSpacing

            // Table Rows
            pdfData.teams.forEach { teamData ->
                currentX = xMargin
                val teamDisplayText = "${teamData.position}. ${teamData.teamName}"
                canvas.drawText(
                    teamDisplayText,
                    currentX + cellPadding,
                    yPosition - textCenterOffsetYPaint,
                    paint
                )
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
                            yPosition - textCenterOffsetYBoldPaint,
                            boldPaint
                        )
                        textX += scoreWidth
                        canvas.drawText(separator, textX, yPosition - textCenterOffsetYPaint, paint)
                        textX += separatorWidth
                        canvas.drawText(
                            strokesString,
                            textX,
                            yPosition - textCenterOffsetYItalicPaint,
                            italicPaint
                        )
                    } else {
                        val scoreText = "-"
                        val textWidth = paint.measureText(scoreText)
                        canvas.drawText(
                            scoreText,
                            currentX + (scoreColWidth - textWidth) / 2,
                            yPosition - textCenterOffsetYPaint,
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
                    yPosition - textCenterOffsetYBoldPaint,
                    boldPaint
                )
                textX += totalScoreWidth
                canvas.drawText(separator, textX, yPosition - textCenterOffsetYPaint, paint)
                textX += separatorWidth
                canvas.drawText(
                    totalStrokesString,
                    textX,
                    yPosition - textCenterOffsetYItalicPaint,
                    italicPaint
                )

                canvas.drawLine(
                    xMargin,
                    yPosition + lineSpacing / 2f,
                    xMargin + availableWidthForTable,
                    yPosition + lineSpacing / 2f,
                    paint
                )
                yPosition += lineSpacing
            }

            val tableBottomY = yPosition - lineSpacing / 2f
            var lineX = xMargin + teamNameColWidth
            canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint)
            (0 until numHoles).forEach { i ->
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
                color = Color.parseColor("#555555") // Dark gray
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LsgScores")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(imageUri, contentValues, null, null)
            }

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

