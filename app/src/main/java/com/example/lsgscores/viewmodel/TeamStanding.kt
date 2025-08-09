package com.example.lsgscores.viewmodel

data class TeamStanding(
    val teamName: String,
    val totalStrokes: Int,
    val totalScore: Int,
    val position: Int
)

data class PlayedHoleDisplay(
    val playedHoleId: Long,  // NEW: ID for deletion
    val holeName: String,
    val position: Int,
    val gameModeName: String,
    val teamResults: List<TeamResult>
)