package fr.centuryspine.lsgscores.utils

import android.content.Context
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.holemode.HoleGameMode

/**
 * Extension functions for HoleGameMode to provide localized descriptions
 */
fun HoleGameMode.getLocalizedDescription(context: Context): String {
    return context.getString(
        when (id) {
            1 -> R.string.game_mode_individual_description
            2 -> R.string.game_mode_scramble_description
            3 -> R.string.game_mode_greensome_description
            4 -> R.string.game_mode_best_ball_description
            else -> throw IllegalArgumentException("Unknown hole game mode ID: $id")
        }
    )
}