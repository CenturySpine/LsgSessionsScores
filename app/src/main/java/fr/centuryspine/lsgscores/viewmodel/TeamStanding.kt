package fr.centuryspine.lsgscores.viewmodel

data class TeamStanding(
    val teamName: String,
    val totalStrokes: Int,
    val totalScore: Int,
    val position: Int,
    // Current session scoring mode id used for display logic (e.g., hide strokes in stroke play)
    val scoringModeId: Int? = null
)

