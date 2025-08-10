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

        // Ajoutez après les constantes de thème existantes
        private const val KEY_LANGUAGE = "selected_language"
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_FRENCH = "fr"
    }

    var selectedTheme: String
        get() = prefs.getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()
}