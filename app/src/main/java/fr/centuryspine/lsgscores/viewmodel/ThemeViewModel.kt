package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _selectedThemeId = MutableStateFlow(appPreferences.selectedTheme)
    val selectedThemeId: StateFlow<String> = _selectedThemeId.asStateFlow()

    fun setTheme(themeId: String) {
        viewModelScope.launch {
            appPreferences.selectedTheme = themeId
            _selectedThemeId.value = themeId
        }
    }
}