package com.example.lsgscores.ui.sessions

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.lsgscores.R
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessionViewModel: SessionViewModel
) {
    val completedSessions by sessionViewModel.completedSessions.collectAsState()
    val context = LocalContext.current

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

            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            val boldPaint = Paint().apply { isFakeBoldText = true; textSize = 10f; }

            var yPosition = 40f 
            val xMargin = 20f
            val lineSpacing = 18f 
            val cellPadding = 5f
            val defaultTextSize = 10f
            paint.textSize = defaultTextSize
            boldPaint.textSize = defaultTextSize // Ensure boldPaint also uses defaultTextSize for metrics

            // Calculate the offset needed to center text vertically based on its metrics.
            // This offset is from the visual center of the line to the baseline of the text.
            val textCenterOffsetYPaint = (paint.ascent() + paint.descent()) / 2f
            val textCenterOffsetYBoldPaint = (boldPaint.ascent() + boldPaint.descent()) / 2f

            // yPosition will now represent the VISUAL CENTER of the current line.
            // Adjust initial yPosition to be the center of the first line
            yPosition += lineSpacing / 2f


            // Session Name
            paint.textSize = 16f // Special larger size for session name
            val sessionNameCenterOffsetY = (paint.ascent() + paint.descent()) / 2f
            canvas.drawText("${context.getString(R.string.pdf_session_name_prefix)} ${pdfData.session.name}", xMargin, yPosition - sessionNameCenterOffsetY, paint)
            yPosition += lineSpacing * 2f 
            paint.textSize = defaultTextSize // Reset to default for subsequent text


            // Basic Session Info (Date, Type - if needed)
            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText("${context.getString(R.string.pdf_start_time_prefix)} ${pdfData.session.dateTime.format(dateTimeFormatter)}", xMargin, yPosition - textCenterOffsetYPaint, paint)
            yPosition += lineSpacing
            pdfData.session.endDateTime?.let {
                canvas.drawText("${context.getString(R.string.pdf_end_time_prefix)} ${it.format(dateTimeFormatter)}", xMargin, yPosition - textCenterOffsetYPaint, paint)
                yPosition += lineSpacing
            }
            canvas.drawText("${context.getString(R.string.pdf_session_type_prefix)} ${pdfData.session.sessionType}", xMargin, yPosition - textCenterOffsetYPaint, paint)
            yPosition += lineSpacing
            pdfData.session.comment?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText("${context.getString(R.string.pdf_comment_prefix)} $it", xMargin, yPosition - textCenterOffsetYPaint, paint)
                yPosition += lineSpacing
            }
            yPosition += lineSpacing // Extra space before table

            // Scores Table
            val numHoles = pdfData.playedHoles.size
            val availableWidthForTable = pageInfo.pageWidth - (2 * xMargin)
            val teamNameColWidth = availableWidthForTable * 0.30f 
            val scoreColWidth = (availableWidthForTable - teamNameColWidth) / numHoles.coerceAtLeast(1)

            // Draw lines relative to yPosition being the center of the row.
            val tableTopY = yPosition - lineSpacing / 2f // Top line of the header row
            
            // Table Headers
            var currentX = xMargin
            canvas.drawText(context.getString(R.string.pdf_header_team_players), currentX + cellPadding, yPosition - textCenterOffsetYBoldPaint, boldPaint)
            currentX += teamNameColWidth

            pdfData.playedHoles.forEach { playedHole ->
                val holeDetail = pdfData.holesDetails[playedHole.holeId]
                val holeName = holeDetail?.name?.takeIf { it.isNotBlank() } ?: "${context.getString(R.string.pdf_hole_prefix)} ${playedHole.position}"
                val textWidth = boldPaint.measureText(holeName)
                canvas.drawText(holeName, currentX + (scoreColWidth - textWidth) / 2, yPosition - textCenterOffsetYBoldPaint, boldPaint)
                currentX += scoreColWidth
            }
            // Line below header
            canvas.drawLine(xMargin, yPosition + lineSpacing / 2f, xMargin + availableWidthForTable, yPosition + lineSpacing / 2f, paint)
            yPosition += lineSpacing // Move to the center of the first data row

            // Table Rows
            pdfData.teamsWithPlayers.forEach { teamWithPlayers ->
                val team = teamWithPlayers.team
                val player1Name = teamWithPlayers.player1?.name ?: ""
                val player2Name = teamWithPlayers.player2?.name?.let { " & $it" } ?: ""
                val teamDisplayName = "$player1Name$player2Name".takeIf { it.isNotBlank() } ?: "Équipe ${team.id}"

                currentX = xMargin
                canvas.drawText(teamDisplayName, currentX + cellPadding, yPosition - textCenterOffsetYPaint, paint)
                currentX += teamNameColWidth

                pdfData.playedHoles.forEach { playedHole ->
                    val scoreKey = Pair(team.id, playedHole.id)
                    val scoreData = pdfData.scores[scoreKey] 
                    val scoreText = scoreData?.let {
                        "${it.calculatedScore} (${it.strokes})"
                    } ?: "-"
                    val textWidth = paint.measureText(scoreText)
                    canvas.drawText(scoreText, currentX + (scoreColWidth - textWidth) / 2, yPosition - textCenterOffsetYPaint, paint)
                    currentX += scoreColWidth
                }
                // Line below data row
                canvas.drawLine(xMargin, yPosition + lineSpacing / 2f, xMargin + availableWidthForTable, yPosition + lineSpacing / 2f, paint)
                yPosition += lineSpacing // Move to the center of the next data row
            }

            // Vertical Lines
            // tableTopY is the top of the header row.
            // The last line drawn was at (yPosition - lineSpacing) + lineSpacing / 2f which is yPosition - lineSpacing / 2f
            // This is the y-coordinate for the bottom of the last data row.
            val tableBottomY = yPosition - lineSpacing / 2f
            var lineX = xMargin + teamNameColWidth
            canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint) // First vertical line after team names
            for (i in 0 until numHoles -1) { // numHoles-1 because the last score column doesn't need a line after it
                 lineX += scoreColWidth
                 canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint)
            }
             // Border lines for the table (re-drawing top and adding side borders based on new coordinates)
            canvas.drawLine(xMargin, tableTopY, xMargin + availableWidthForTable, tableTopY, paint) // Top border
            canvas.drawLine(xMargin, tableTopY, xMargin, tableBottomY, paint) // Left border
            canvas.drawLine(xMargin + availableWidthForTable, tableTopY, xMargin + availableWidthForTable, tableBottomY, paint) // Right border
            // Bottom border is already drawn by the last data row's line.

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

// Ensure you have these string resources in your strings.xml:
// <string name="session_history_title">Historique des sessions</string> (This one is used in TopAppBar)
// <string name="export_session_to_pdf_content_description">Exporter la session en PDF</string>
// <string name="share_session_pdf_title">Partager le PDF de la session via</string>
// <string name="pdf_session_name_prefix">Session:</string>
// <string name="pdf_start_time_prefix">Début:</string>
// <string name="pdf_end_time_prefix">Fin:</string>
// <string name="pdf_session_type_prefix">Type:</string>
// <string name="pdf_comment_prefix">Commentaire:</string>
// <string name="exporting_pdf_toast_message">Exportation du PDF en cours...</string>
// <string name="pdf_generation_failed_toast_message">Échec de la génération du PDF.</string>
// <string name="session_history_empty_title">Aucune session terminée</string>
// <string name="session_history_empty_message">Terminez une session pour la voir ici.</string>
// <string name="session_history_date_format_pattern">EEEE d MMMM yyyy</string>
// <string name="session_history_time_prefix">Heure:</string>
// <string name="session_history_duration_hours_minutes">"%1$d h %2$d min"</string>
// <string name="session_history_duration_hours">"%1$d h"</string>
// <string name="session_history_duration_minutes">"%1$d min"</string>
// <string name="session_history_separator">|</string>
// <string name="session_history_duration_prefix">Durée:</string>
// <string name="pdf_header_team_players">Équipe/Joueurs</string>
// <string name="pdf_hole_prefix">Trou</string>