package com.example.lsgscores.data.scoring
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ScoringModeRepository {

    // Hardcoded list of scoring modes
    private val scoringModes = listOf(
        ScoringMode(
            id = 1,
            name = "Classic (Fewest Strokes Wins)",
            description = "Each player's or team's score is the total number of strokes. The lowest total wins."
        ),
        ScoringMode(
            id = 2,
            name = "Single Point for Best Score",
            description = "On each hole, the player or team with the lowest unique number of strokes scores 1 point. All others score 0 points."
        ),
        ScoringMode(
            id = 3,
            name = "First Alone Gets 3, Tied Leaders Get 2",
            description = "If a single player or team has the lowest number of strokes, they get 3 points and others get 0. If multiple tie for lowest, each gets 2 points and others get 0."
        ),
        ScoringMode(
            id = 4,
            name = "2 Points for First, 1 for Solo Second",
            description = "If a single player is first, they get 2 points and a solo second gets 1 point. If two tie for first, they get 1 point each and a solo second gets 1 point. Three or more tied for first: nobody gets any points."
        )
        // Add more scoring modes here as needed
    )

    fun getAll(): Flow<List<ScoringMode>> = flowOf(scoringModes)

    fun getById(id: Int): Flow<ScoringMode?> = flowOf(scoringModes.find { it.id == id })
}
