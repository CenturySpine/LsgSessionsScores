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
import com.example.lsgscores.R
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.viewmodel.SessionViewModel
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
                    SessionHistoryCard(
                        session = session,
                        onExportClick = { selectedSession ->
                            Toast.makeText(context, context.getString(R.string.exporting_pdf_toast_message), Toast.LENGTH_SHORT).show()
                            generateAndSharePdf(context, selectedSession)
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
            IconButton(onClick = { onExportClick(session) }) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = stringResource(R.string.export_session_to_pdf_content_description)
                )
            }
        }
    }
}

private fun generateAndSharePdf(context: Context, session: Session) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    var yPosition = 40f
    val xMargin = 20f
    val lineSpacing = 20f

    paint.textSize = 16f
    canvas.drawText("${context.getString(R.string.pdf_session_name_prefix)} ${session.name}", xMargin, yPosition, paint)
    yPosition += lineSpacing * 2 // Extra space after title

    paint.textSize = 12f
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())

    canvas.drawText("${context.getString(R.string.pdf_start_time_prefix)} ${session.dateTime.format(dateTimeFormatter)}", xMargin, yPosition, paint)
    yPosition += lineSpacing

    session.endDateTime?.let {
        canvas.drawText("${context.getString(R.string.pdf_end_time_prefix)} ${it.format(dateTimeFormatter)}", xMargin, yPosition, paint)
        yPosition += lineSpacing
    }

    canvas.drawText("${context.getString(R.string.pdf_session_type_prefix)} ${session.sessionType}", xMargin, yPosition, paint)
    yPosition += lineSpacing

    session.comment?.takeIf { it.isNotBlank() }?.let {
        canvas.drawText("${context.getString(R.string.pdf_comment_prefix)} $it", xMargin, yPosition, paint)
    }

    pdfDocument.finishPage(page)

    // Save the PDF
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val fileName = "session_${session.id}_${timeStamp}.pdf"
    val pdfDir = File(context.cacheDir, "pdfs")
    if (!pdfDir.exists()) {
        pdfDir.mkdirs()
    }
    val pdfFile = File(pdfDir, fileName)

    try {
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
        Toast.makeText(context, context.getString(R.string.pdf_generation_failed_toast_message), Toast.LENGTH_LONG).show()
    } finally {
        pdfDocument.close()
    }
}

// Ensure you have these string resources in your strings.xml:
// <string name="session_history_title">Historique des sessions</string>
// <string name="export_session_to_pdf_content_description">Exporter la session en PDF</string>
// <string name="share_session_pdf_title">Partager le PDF de la session via</string>
// <string name="pdf_session_name_prefix">Session:</string>
// <string name="pdf_start_time_prefix">Début:</string>
// <string name="pdf_end_time_prefix">Fin:</string>
// <string name="pdf_session_type_prefix">Type:</string>
// <string name="pdf_comment_prefix">Commentaire:</string>
// <string name="exporting_pdf_toast_message">Exportation du PDF en cours...</string>
// <string name="pdf_generation_failed_toast_message">Échec de la génération du PDF.</string>
// Also ensure R.string.session_history_empty_title, R.string.session_history_empty_message,
// R.string.session_history_date_format_pattern, R.string.session_history_time_prefix,
// R.string.session_history_duration_hours_minutes, R.string.session_history_duration_hours,
// R.string.session_history_duration_minutes, R.string.session_history_separator, R.string.session_history_duration_prefix
// are defined.
