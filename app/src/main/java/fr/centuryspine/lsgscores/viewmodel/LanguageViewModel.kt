package fr.centuryspine.lsgscores.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(appPreferences.selectedLanguage)
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()


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
}