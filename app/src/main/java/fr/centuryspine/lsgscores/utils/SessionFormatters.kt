package fr.centuryspine.lsgscores.utils

import android.content.Context
import fr.centuryspine.lsgscores.R
import java.time.Duration
import java.time.LocalDateTime

/**
 * Utility helpers to format session-related values for display.
 * All display strings are sourced from string resources to support localization.
 */
object SessionFormatters {
    /**
     * Format the duration between [start] and [end] using localized strings.
     * If the duration is less than one hour, only minutes are shown.
     */
    fun formatSessionDuration(context: Context, start: LocalDateTime, end: LocalDateTime): String {
        val duration = Duration.between(start, end)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return when {
            hours > 0 -> if (minutes > 0) {
                context.getString(R.string.session_history_duration_hours_minutes, hours, minutes)
            } else {
                context.getString(R.string.session_history_duration_hours, hours)
            }

            else -> context.getString(R.string.session_history_duration_minutes, minutes)
        }
    }
}
