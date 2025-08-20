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
import java.text.SimpleDateFormat // Keep for timestamp in filename
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


private fun generateAndShareMarkdown(
    context: Context,
    session: Session,
    sessionViewModel: SessionViewModel
) {
    sessionViewModel.viewModelScope.launch {
        try {
            // TODO: Ajouter une nouvelle ressource string pour ce message
            Toast.makeText(context, "Exportation Markdown en cours...", Toast.LENGTH_SHORT).show()
            val pdfData = sessionViewModel.loadSessionPdfData(session).first() // réutilisation de la même structure de données

            val markdownContent = StringBuilder()

            // Session Name
            markdownContent.append("# ${context.getString(R.string.pdf_session_name_prefix)} ${pdfData.session.name}\n\n")

            // Basic Session Info
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

            markdownContent.append("**${context.getString(R.string.pdf_label_session_date)}** ${pdfData.session.dateTime.format(dateFormatter)}\n")
            markdownContent.append("**${context.getString(R.string.pdf_label_session_start_time)}** ${pdfData.session.dateTime.format(timeFormatter)}\n")
            val endTimeText = pdfData.session.endDateTime?.format(timeFormatter) ?: context.getString(R.string.pdf_not_applicable)
            markdownContent.append("**${context.getString(R.string.pdf_label_session_end_time)}** $endTimeText\n")
            markdownContent.append("**${context.getString(R.string.pdf_session_type_prefix)}** ${pdfData.session.sessionType}\n")
            pdfData.session.comment?.takeIf { it.isNotBlank() }?.let {
                markdownContent.append("**${context.getString(R.string.pdf_comment_prefix)}** $it\n")
            }
            markdownContent.append("\n") // Extra newline before table

            // Scores Table
            // Headers
            markdownContent.append("| ${context.getString(R.string.pdf_header_team_players)} |")
            pdfData.playedHoles.forEach { playedHole ->
                val holeDetail = pdfData.holesDetails[playedHole.holeId]
                val holeName = holeDetail?.name?.takeIf { it.isNotBlank() } ?: "${context.getString(R.string.pdf_hole_prefix)} ${playedHole.position}"
                markdownContent.append(" $holeName |")
            }
            markdownContent.append("\n")

            // Separator line (Markdown table syntax)
            markdownContent.append("|:---|") // Left-align team names
            pdfData.playedHoles.forEach { _ ->
                markdownContent.append(":---:|") // Center-align scores
            }
            markdownContent.append("\n")

            // Table Rows
            pdfData.teamsWithPlayers.forEach { teamWithPlayers ->
                val team = teamWithPlayers.team
                val player1Name = teamWithPlayers.player1?.name ?: ""
                val player2Name = teamWithPlayers.player2?.name?.let { " & $it" } ?: ""
                val teamDisplayName = "$player1Name$player2Name".takeIf { it.isNotBlank() } ?: "${context.getString(R.string.pdf_team_prefix)} ${team.id}"

                markdownContent.append("| $teamDisplayName |")

                pdfData.playedHoles.forEach { playedHole ->
                    val scoreKey = Pair(team.id, playedHole.id)
                    val scoreData = pdfData.scores[scoreKey]
                    val scoreText = scoreData?.let {
                        "${it.calculatedScore} (${it.strokes})"
                    } ?: "-"
                    markdownContent.append(" $scoreText |")
                }
                markdownContent.append("\n")
            }

            // Save and Share
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
            val fileName = "session_${pdfData.session.id}_${timeStamp}.md"
            val mdDir = File(context.cacheDir, "markdowns") // Nouveau dossier pour les exports md
            if (!mdDir.exists()) {
                mdDir.mkdirs()
            }
            val mdFile = File(mdDir, fileName)
            mdFile.writeText(markdownContent.toString())

            val mdUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mdFile)
            // TODO: Ajouter une nouvelle ressource string pour ce titre de partage
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown" // Ou "text/plain" pour une compatibilité plus large
                putExtra(Intent.EXTRA_STREAM, mdUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Optionnel: ajouter un sujet
                // putExtra(Intent.EXTRA_SUBJECT, "Export de session LsgScores")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager le fichier Markdown via..."))

        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: Ajouter une nouvelle ressource string pour ce message d'erreur
            Toast.makeText(context, "Échec de la génération Markdown: ${e.message}", Toast.LENGTH_LONG).show()
        }
        // Pas de 'finally { pdfDocument?.close() }' nécessaire car nous n'utilisons pas PdfDocument
    }
}

// TODO: N'oubliez pas d'ajouter les nouvelles ressources string à votre strings.xml :
// <string name="exporting_markdown_toast_message">Exportation Markdown en cours...</string>
// <string name="share_session_md_title">Partager le Markdown de la session via</string>
// <string name="md_generation_failed_toast_message">Échec de la génération du Markdown.</string>

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
            val boldPaint = Paint().apply { isFakeBoldText = true }

            var yPosition = 40f
            val xMargin = 20f
            val lineSpacing = 18f
            val cellPadding = 5f
            val defaultTextSize = 10f
            
            paint.textSize = defaultTextSize
            boldPaint.textSize = defaultTextSize

            // Calculate ascent/descent for vertical centering AFTER setting textSize
            val textCenterOffsetYPaint = (paint.ascent() + paint.descent()) / 2f
            val textCenterOffsetYBoldPaint = (boldPaint.ascent() + boldPaint.descent()) / 2f

            yPosition += lineSpacing / 2f


            // Session Name
            paint.textSize = 16f // Temporarily increase for session name
            boldPaint.textSize = 16f
            val sessionNameLabel = "${context.getString(R.string.pdf_session_name_prefix)} "
            val sessionNameLabelWidth = boldPaint.measureText(sessionNameLabel)
            val sessionNameTextCenterOffsetYLarge = (boldPaint.ascent() + boldPaint.descent()) / 2f // For 16f

            canvas.drawText(sessionNameLabel, xMargin, yPosition - sessionNameTextCenterOffsetYLarge, boldPaint)
            canvas.drawText(pdfData.session.name, xMargin + sessionNameLabelWidth, yPosition - sessionNameTextCenterOffsetYLarge, paint)
            
            paint.textSize = defaultTextSize // Reset to default
            boldPaint.textSize = defaultTextSize
            yPosition += lineSpacing * 2f


            // Basic Session Info
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

            // Session Date
            var labelText = "${context.getString(R.string.pdf_label_session_date)} "
            var labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(labelText, xMargin, yPosition - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(pdfData.session.dateTime.format(dateFormatter), xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint)
            yPosition += lineSpacing

            // Session Start Time
            labelText = "${context.getString(R.string.pdf_label_session_start_time)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(labelText, xMargin, yPosition - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(pdfData.session.dateTime.format(timeFormatter), xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint)
            yPosition += lineSpacing

            // Session End Time
            labelText = "${context.getString(R.string.pdf_label_session_end_time)} "
            labelWidth = boldPaint.measureText(labelText)
            val endTimeValue = pdfData.session.endDateTime?.format(timeFormatter) ?: context.getString(R.string.pdf_not_applicable)
            canvas.drawText(labelText, xMargin, yPosition - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(endTimeValue, xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint)
            yPosition += lineSpacing
            
            // Session Type
            labelText = "${context.getString(R.string.pdf_session_type_prefix)} "
            labelWidth = boldPaint.measureText(labelText)
            canvas.drawText(labelText, xMargin, yPosition - textCenterOffsetYBoldPaint, boldPaint)
            canvas.drawText(pdfData.session.sessionType.toString(), xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint) // Assuming sessionType can be .toString()
            yPosition += lineSpacing
            
            // Session Comment
            pdfData.session.comment?.takeIf { it.isNotBlank() }?.let {
                labelText = "${context.getString(R.string.pdf_comment_prefix)} "
                labelWidth = boldPaint.measureText(labelText)
                canvas.drawText(labelText, xMargin, yPosition - textCenterOffsetYBoldPaint, boldPaint)
                canvas.drawText(it, xMargin + labelWidth, yPosition - textCenterOffsetYPaint, paint)
                yPosition += lineSpacing
            }
            yPosition += lineSpacing // Extra space before table

            // Scores Table
            val numHoles = pdfData.playedHoles.size
            val availableWidthForTable = pageInfo.pageWidth - (2 * xMargin)
            val teamNameColWidth = availableWidthForTable * 0.30f
            val scoreColWidth = (availableWidthForTable - teamNameColWidth) / numHoles.coerceAtLeast(1)

            val tableTopY = yPosition - lineSpacing / 2f
            
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
            canvas.drawLine(xMargin, yPosition + lineSpacing / 2f, xMargin + availableWidthForTable, yPosition + lineSpacing / 2f, paint)
            yPosition += lineSpacing

            // Table Rows
            pdfData.teamsWithPlayers.forEach { teamWithPlayers ->
                val team = teamWithPlayers.team
                val player1Name = teamWithPlayers.player1?.name ?: ""
                val player2Name = teamWithPlayers.player2?.name?.let { " & $it" } ?: ""
                val teamDisplayName = "$player1Name$player2Name".takeIf { it.isNotBlank() } ?: "${context.getString(R.string.pdf_team_prefix)} ${team.id}"

                currentX = xMargin
                canvas.drawText(teamDisplayName, currentX + cellPadding, yPosition - textCenterOffsetYPaint, paint)
                currentX += teamNameColWidth

                pdfData.playedHoles.forEach { playedHole ->
                    val scoreKey = Pair(team.id, playedHole.id)
                    val scoreData = pdfData.scores[scoreKey]
                    val scoreText = scoreData?.let {
                        "${it.strokes} - ${it.calculatedScore}"
                    } ?: "-"
                    val textWidth = paint.measureText(scoreText)
                    canvas.drawText(scoreText, currentX + (scoreColWidth - textWidth) / 2, yPosition - textCenterOffsetYPaint, paint)
                    currentX += scoreColWidth
                }
                canvas.drawLine(xMargin, yPosition + lineSpacing / 2f, xMargin + availableWidthForTable, yPosition + lineSpacing / 2f, paint)
                yPosition += lineSpacing
            }

            val tableBottomY = yPosition - lineSpacing / 2f 
            var lineX = xMargin + teamNameColWidth
            canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint)
            for (i in 0 until numHoles -1) { // Draw lines between score columns
                 lineX += scoreColWidth
                 canvas.drawLine(lineX, tableTopY, lineX, tableBottomY, paint)
            }
            // Draw table borders
            canvas.drawLine(xMargin, tableTopY, xMargin + availableWidthForTable, tableTopY, paint) // Top border
            canvas.drawLine(xMargin, tableTopY, xMargin, tableBottomY, paint) // Left border
            canvas.drawLine(xMargin + availableWidthForTable, tableTopY, xMargin + availableWidthForTable, tableBottomY, paint) // Right border
            // Bottom border is already drawn by the last row's line

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
