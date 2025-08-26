package fr.centuryspine.lsgscores.utils

import android.content.Context
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.scoring.ScoringMode

/**
 * Extension functions for ScoringMode to provide localized names and descriptions
 */
fun ScoringMode.getLocalizedName(context: Context): String {
    return context.getString(when (id) {
        1 -> R.string.scoring_mode_classic_name
        2 -> R.string.scoring_mode_strict_name
        3 -> R.string.scoring_mode_no_second_place_name
        4 -> R.string.scoring_mode_redistribution_name
        else -> throw IllegalArgumentException("Unknown scoring mode ID: $id")
    })
}

fun ScoringMode.getLocalizedDescription(context: Context): String {
    return context.getString(when (id) {
        1 -> R.string.scoring_mode_classic_description
        2 -> R.string.scoring_mode_strict_description
        3 -> R.string.scoring_mode_no_second_place_description
        4 -> R.string.scoring_mode_redistribution_description
        else -> throw IllegalArgumentException("Unknown scoring mode ID: $id")
    })
}