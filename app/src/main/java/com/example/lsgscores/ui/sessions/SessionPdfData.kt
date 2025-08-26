package com.example.lsgscores.ui.sessions

import com.example.lsgscores.data.gamezone.GameZone
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.session.PlayedHole
import com.example.lsgscores.data.session.Session

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