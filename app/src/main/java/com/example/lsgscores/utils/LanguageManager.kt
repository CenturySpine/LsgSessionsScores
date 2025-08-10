package com.example.lsgscores.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.lsgscores.data.preferences.AppPreferences
import java.util.Locale

object LanguageManager {

    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            AppPreferences.LANGUAGE_SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    Locale.getDefault()
                }
            }
            AppPreferences.LANGUAGE_FRENCH -> Locale("fr")
            AppPreferences.LANGUAGE_ENGLISH -> Locale("en")
            else -> Locale("en") // Default fallback
        }

        return updateContextLocale(context, locale)
    }

    private fun updateContextLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }

    fun getLocaleFromLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            AppPreferences.LANGUAGE_FRENCH -> Locale("fr")
            AppPreferences.LANGUAGE_ENGLISH -> Locale("en")
            else -> Locale.getDefault()
        }
    }
}