import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

object ExportHelpers {


    fun generateAndShareImageExport(
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
                // In stroke play (id = 1), hide strokes/coups in the image export
                val hideStrokes = (pdfData.session.scoringModeId == 1)

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
                var resultsY = bottomMargin - 40f // Leave space for footer

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

                    val scoreText = if (hideStrokes) {
                        "${teamData.totalCalculatedScore}"
                    } else {
                        "${teamData.totalCalculatedScore} - ${teamData.totalStrokes}"
                    }
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

    fun generateAndSharePdf(
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
                val scoringMode =
                    try {
                        sessionViewModel.scoringMode(pdfData.session.scoringModeId)
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

                // In stroke play (id = 1), hide strokes/coups values in the table
                val hideStrokes = (pdfData.session.scoringModeId == 1)

                // Helper to wrap text within a max width
                fun wrapText(text: String, p: Paint, maxWidth: Float): List<String> {
                    if (text.isBlank()) return listOf("")
                    val words = text.trim().split(" ")
                    val lines = mutableListOf<String>()
                    var current = StringBuilder()
                    for (word in words) {
                        val candidate = if (current.isEmpty()) word else current.toString() + " " + word
                        if (p.measureText(candidate) <= maxWidth) {
                            if (current.isEmpty()) current.append(word) else {
                                current.append(" "); current.append(word)
                            }
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
                    val holeName = holeDetail?.name?.takeIf { it.isNotBlank() }
                        ?: "${context.getString(R.string.pdf_hole_prefix)} ${playedHole.position}"
                    val gameModeName = pdfData.holeGameModes[playedHole.gameModeId.toLong()] ?: ""
                    HeaderCol(wrapText(holeName, boldPaint, headerCellInnerWidth), gameModeName)
                }
                val maxNameLines = headerCols.maxOfOrNull { it.wrappedNameLines.size } ?: 1

                // Compute dynamic header height considering both hole name lines + game mode and wrapped left header
                val teamHeaderInnerWidth = teamNameColWidth - 2 * cellPadding
                val teamHeaderLines =
                    wrapText(context.getString(R.string.pdf_header_team_players), boldPaint, teamHeaderInnerWidth)
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
                            if (hideStrokes) {
                                // Only draw calculated score, centered
                                val scoreWidth = boldPaint.measureText(scoreString)
                                val textX = currentX + (scoreColWidth - scoreWidth) / 2
                                canvas.drawText(
                                    scoreString,
                                    textX,
                                    rowCenterY - textCenterOffsetYBoldPaint,
                                    boldPaint
                                )
                            } else {
                                // Draw score - (strokes)
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
                            }
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
                    if (hideStrokes) {
                        // Only total score, centered
                        val totalScoreWidth = boldPaint.measureText(totalScoreString)
                        val textX = currentX + (totalColWidth - totalScoreWidth) / 2
                        canvas.drawText(
                            totalScoreString,
                            textX,
                            rowCenterY - textCenterOffsetYBoldPaint,
                            boldPaint
                        )
                    } else {
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
                    }

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
}