package fr.centuryspine.lsgscores.ui.sessions

import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.session.PlayedHole
import fr.centuryspine.lsgscores.data.session.Session

data class PdfScoreDisplayData(
    val strokes: Int,
    val calculatedScore: Int
)

data class SessionPdfData(
    val session: Session,
    val teams: List<TeamPdfData>, // Sorted by total calculated score (descending)
    val playedHoles: List<PlayedHole>,
    val holesDetails: Map<Long, Hole>,
    val gameZone: GameZone?,
    val holeGameModes: Map<Long, String>
)