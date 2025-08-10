package com.example.lsgscores.viewmodel

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.R
import com.example.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class LanguageOption(
    val code: String,
    val displayName: String,
    val flagEmoji: String
)

@HiltViewModel
class LanguageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(appPreferences.selectedLanguage)
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _currentSystemLanguage = MutableStateFlow(getCurrentSystemLanguage())
    val currentSystemLanguage: StateFlow<String> = _currentSystemLanguage.asStateFlow()

    // Remplacez la propriété availableLanguages par cette fonction
    fun getAvailableLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption(
                code = AppPreferences.LANGUAGE_SYSTEM,
                displayName = context.getString(R.string.settings_language_system),
                flagEmoji = "🌐"
            ),
            LanguageOption(
                code = AppPreferences.LANGUAGE_ENGLISH,
                displayName = context.getString(R.string.settings_language_english),
                flagEmoji = "🇺🇸"
            ),
            LanguageOption(
                code = AppPreferences.LANGUAGE_FRENCH,
                displayName = context.getString(R.string.settings_language_french),
                flagEmoji = "🇫🇷"
            )
        )
    }
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            appPreferences.selectedLanguage = languageCode
            _selectedLanguage.value = languageCode
        }
    }

    fun getCurrentDisplayLanguage(): String {
        return when (val selected = _selectedLanguage.value) {
            AppPreferences.LANGUAGE_SYSTEM -> getCurrentSystemLanguage()
            else -> selected
        }
    }

    private fun getCurrentSystemLanguage(): String {
        val systemLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return when (systemLocale.language) {
            "fr" -> AppPreferences.LANGUAGE_FRENCH
            "en" -> AppPreferences.LANGUAGE_ENGLISH
            else -> AppPreferences.LANGUAGE_ENGLISH // Default fallback
        }
    }

    fun updateSystemLanguage() {
        _currentSystemLanguage.value = getCurrentSystemLanguage()
    }
}