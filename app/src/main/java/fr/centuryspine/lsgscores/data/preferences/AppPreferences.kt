package fr.centuryspine.lsgscores.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "lsg_scores_prefs",
        Context.MODE_PRIVATE
    )

    private val _selectedCityIdFlow = MutableStateFlow(getSelectedCityId())
    val selectedCityIdFlow: StateFlow<Long?> = _selectedCityIdFlow.asStateFlow()

    companion object {
        private const val KEY_THEME = "selected_theme"
        const val THEME_DEFAULT = "default"

        private const val KEY_LANGUAGE = "selected_language"
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_FRENCH = "fr"

        private const val KEY_SELECTED_CITY = "selected_city_id"
    }

    var selectedTheme: String
        get() = prefs.getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT
        set(value) = prefs.edit { putString(KEY_THEME, value) }

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    fun getSelectedCityId(): Long? {
        val cityId = prefs.getLong(KEY_SELECTED_CITY, -1L)
        return if (cityId == -1L) null else cityId
    }

    fun setSelectedCityId(cityId: Long) {
        prefs.edit { putLong(KEY_SELECTED_CITY, cityId) }
        _selectedCityIdFlow.value = cityId
    }

}