package fr.centuryspine.lsgscores.ui.sessions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat // Keep for timestamp in filename
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import fr.centuryspine.lsgscores.R
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessionViewModel: SessionViewModel
) {
    val completedSessions by sessionViewModel.completedSessions.collectAsState()
    val context = LocalContext.current

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
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    onExportClick: (Session) -> Unit,
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
                            hours > 0 -> if (minutes > 0) stringResource(R.string.session_history_duration_hours_minutes, hours, minutes)
                            else stringResource(R.string.session_history_duration_hours, hours)
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
            IconButton(onClick = { onExportClick(session) }) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = stringResource(R.string.export_session_to_pdf_content_description)
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
            Toast.makeText(context, context.getString(R.string.exporting_pdf_toast_message), Toast.LENGTH_SHORT).show()
            val pdfData = sessionViewModel.loadSessionPdfData(session).first()
            val weatherInfo = sessionViewModel.getCurrentWeatherInfo()

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
            val sessionNameTextCenterOffsetYLarge = (boldPaint.ascent() + boldPaint.descent()) / 2f // For 16f

            canvas.drawText(sessionNameLabel, xMargin, yPosition - sessionNameTextCenterOffsetYLarge, boldPaint)
            canvas.drawText(pdfData.gameZone?.name ?: "N/A", xMargin + sessionNameLabelWidth, yPosition - sessionNameTextCenterOffsetYLarge, paint)

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
            canvas.drawText(labelText, leftColumnX, currentY - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(pdfData.session.dateTime.format(dateFormatter), leftColumnX + labelWidth, currentY - textCenterOffsetYPaint, paint)
            currentY += lineSpacing

            // Session Start Time
            labelText = "${context.getString(R.string.pdf_label_session_start_time)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(labelText, leftColumnX, currentY - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(pdfData.session.dateTime.format(timeFormatter), leftColumnX + labelWidth, currentY - textCenterOffsetYPaint, paint)
            currentY += lineSpacing

            // Session End Time
            labelText = "${context.getString(R.string.pdf_label_session_end_time)} "
            labelWidth = boldPaint.measureText(labelText)
            val endTimeValue = pdfData.session.endDateTime?.format(timeFormatter) ?: context.getString(R.string.pdf_not_applicable)
            canvas.drawText(labelText, leftColumnX, currentY - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(endTimeValue, leftColumnX + labelWidth, currentY - textCenterOffsetYPaint, paint)
            currentY += lineSpacing

            // Session Type
            labelText = "${context.getString(R.string.pdf_session_type_prefix)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(labelText, leftColumnX, currentY - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(pdfData.session.sessionType.toString(), leftColumnX + labelWidth, currentY - textCenterOffsetYPaint, paint)
            currentY += lineSpacing

            // RIGHT COLUMN - Weather Info
            if (weatherInfo != null) {
                var weatherY = yPosition // Start from same Y as left column

                // Weather title
                val weatherTitle = context.getString(R.string.pdf_weather_title)
                canvas.drawText(weatherTitle, rightColumnX, weatherY - textCenterOffsetYBoldPaint, boldPaint)
                weatherY += lineSpacing

                // Temperature
                val tempText = "${weatherInfo.temperature}°C"
                canvas.drawText(tempText, rightColumnX, weatherY - textCenterOffsetYPaint, paint)
                weatherY += lineSpacing

                // Weather description
                canvas.drawText(weatherInfo.description, rightColumnX, weatherY - textCenterOffsetYPaint, italicPaint)
                weatherY += lineSpacing

                // Wind
                val windText = "${context.getString(R.string.pdf_wind_label)} ${weatherInfo.windSpeedKmh} km/h"
                canvas.drawText(windText, rightColumnX, weatherY - textCenterOffsetYPaint, paint)
            }

            // Update yPosition to after both columns
            yPosition = currentY

            // Session Comment (full width, below both columns)
            pdfData.session.comment?.takeIf { it.isNotBlank() }?.let {
                labelText = "${context.getString(R.string.pdf_comment_prefix)} "
                labelWidth = boldPaint.measureText(labelText)
                canvas.drawText(labelText, xMargin, yPosition - textCenterOffsetYBoldPaint, boldPaint)
                canvas.drawText(it, xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint)
                yPosition += lineSpacing
            }
            yPosition += lineSpacing // Extra space before table


            // Scores Table

            val teamNameColWidth = availableWidthForTable * 0.25f
            val totalColWidth = availableWidthForTable * 0.15f
            val scoreColWidth = (availableWidthForTable - teamNameColWidth - totalColWidth) / numHoles.coerceAtLeast(1)

            val tableHeaderHeight = lineSpacing * 1.5f // Increased height for two lines

            val tableTopY = yPosition - tableHeaderHeight / 2f + 4f


            // Table Headers
            var currentX = xMargin
            val headerCenterY = tableTopY + tableHeaderHeight / 2f
            canvas.drawText(context.getString(R.string.pdf_header_team_players), currentX + cellPadding, headerCenterY - textCenterOffsetYBoldPaint, boldPaint)
            currentX += teamNameColWidth

            pdfData.playedHoles.forEach { playedHole ->
                val holeDetail = pdfData.holesDetails[playedHole.holeId]
                val holeName = holeDetail?.name?.takeIf { it.isNotBlank() } ?: "${context.getString(R.string.pdf_hole_prefix)} ${playedHole.position}"
                val gameModeName = pdfData.holeGameModes[playedHole.gameModeId.toLong()] ?: ""

                val holeNameWidth = boldPaint.measureText(holeName)
                val gameModeNameWidth = gameModePaint.measureText(gameModeName)

                // Draw Hole Name (top part of the cell)
                canvas.drawText(holeName, currentX + (scoreColWidth - holeNameWidth) / 2, headerCenterY - (lineSpacing/4) - textCenterOffsetYBoldPaint, boldPaint)
                // Draw Game Mode Name (bottom part of the cell)
                canvas.drawText(gameModeName, currentX + (scoreColWidth - gameModeNameWidth) / 2, headerCenterY + (lineSpacing/2) - textCenterOffsetYBoldPaint, gameModePaint)

                currentX += scoreColWidth
            }
            canvas.drawText(context.getString(R.string.pdf_header_total), currentX + (totalColWidth - boldPaint.measureText(context.getString(R.string.pdf_header_total))) / 2, headerCenterY - textCenterOffsetYBoldPaint, boldPaint)

            yPosition = tableTopY + tableHeaderHeight - 4f
            canvas.drawLine(xMargin, yPosition + lineSpacing / 2f, xMargin + availableWidthForTable, yPosition + lineSpacing / 2f, paint)
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
                        canvas.drawText(scoreString, textX, yPosition - textCenterOffsetYBoldPaint, boldPaint)
                        textX += scoreWidth
                        canvas.drawText(separator, textX, yPosition - textCenterOffsetYPaint, paint)
                        textX += separatorWidth
                        canvas.drawText(strokesString, textX, yPosition - textCenterOffsetYItalicPaint, italicPaint)
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
                canvas.drawText(totalScoreString, textX, yPosition - textCenterOffsetYBoldPaint, boldPaint)
                textX += totalScoreWidth
                canvas.drawText(separator, textX, yPosition - textCenterOffsetYPaint, paint)
                textX += separatorWidth
                canvas.drawText(totalStrokesString, textX, yPosition - textCenterOffsetYItalicPaint, italicPaint)

                canvas.drawLine(xMargin, yPosition + lineSpacing / 2f, xMargin + availableWidthForTable, yPosition + lineSpacing / 2f, paint)
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
            canvas.drawLine(xMargin, tableTopY, xMargin + availableWidthForTable, tableTopY, paint) // Top border
            canvas.drawLine(xMargin, tableTopY, xMargin, tableBottomY, paint) // Left border
            canvas.drawLine(xMargin + availableWidthForTable, tableTopY, xMargin + availableWidthForTable, tableBottomY, paint) // Right border
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

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
            val fileName = "session_${pdfData.session.id}_${timeStamp}.pdf"
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val pdfFile = File(pdfDir, fileName)

            FileOutputStream(pdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }

            val pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_session_pdf_title)))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "${context.getString(R.string.pdf_generation_failed_toast_message)} ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument?.close()
        }
    }
}
