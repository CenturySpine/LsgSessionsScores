package fr.centuryspine.lsgscores.ui.sessions

data class TeamPdfData(
    val teamId: Long,
    val teamName: String,
    val holeScores: Map<Long, PdfScoreDisplayData>,
    val totalStrokes: Int,
    val totalCalculatedScore: Int,
    val position: Int = 0  // Position in the ranking
)