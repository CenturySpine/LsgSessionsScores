package com.example.lsgscores.ui.sessions

import com.example.lsgscores.data.gamezone.GameZone // Added import for GameZone
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.session.PlayedHole
import com.example.lsgscores.data.session.PlayedHoleScore
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.data.session.TeamWithPlayers

data class PdfScoreDisplayData(
    val strokes: Int,
    val calculatedScore: Int
)

data class SessionPdfData(
    val session: Session,
    val teamsWithPlayers: List<TeamWithPlayers>,
    val playedHoles: List<PlayedHole>,
    val holesDetails: Map<Long, Hole>, // Map de playedHole.holeId vers Hole
    val scores: Map<Pair<Long, Long>, PdfScoreDisplayData>, // Map de (teamId, playedHoleId) vers PdfScoreDisplayData
    val gameZone: GameZone? // Added gameZone parameter
)