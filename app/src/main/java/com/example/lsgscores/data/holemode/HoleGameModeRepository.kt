package com.example.lsgscores.data.holemode

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class HoleGameModeRepository {

    // Hardcoded list of game modes
    private val holeGameModes = listOf(
        HoleGameMode(
            id = 1,
            name = "Individual",
            description = "Each player plays their own ball. The lowest number of strokes wins the hole."
        ),
        HoleGameMode(
            id = 2,
            name = "Scramble",
            description = "Each player in the team plays a ball, then the best ball is chosen for the next shot. The team continues from the position of the best shot."
        ),
        HoleGameMode(
            id = 3,
            name = "Greensome",
            description = "Both players in the team tee off, select the best drive, then alternate shots with the selected ball until the hole is completed."
        ),
        HoleGameMode(
            id = 4,
            name = "Best Ball",
            description = "Each player in the team plays their own ball on each hole. The best score among teammates is the team’s score for that hole."
        )
    )

    fun getAll(): Flow<List<HoleGameMode>> = flowOf(holeGameModes)

    fun getById(id: Int): Flow<HoleGameMode?> = flowOf(holeGameModes.find { it.id == id })
}
