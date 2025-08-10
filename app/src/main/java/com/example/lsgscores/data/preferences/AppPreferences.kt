package com.example.lsgscores.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "lsg_scores_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_THEME = "selected_theme"
        const val THEME_DEFAULT = "default"
        const val THEME_MATERIAL_IO = "material_io"
        const val THEME_OCEAN = "ocean"
        const val THEME_SUNSET = "sunset"
    }

    var selectedTheme: String
        get() = prefs.getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()
}