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

        // Participant mode prefs
        private const val KEY_PARTICIPANT_MODE = "participant_mode"
        private const val KEY_PARTICIPANT_SESSION_ID = "participant_session_id"
        private const val KEY_PARTICIPANT_TEAM_ID = "participant_team_id"
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

    // Participant state helpers
    private val _participantModeFlow = MutableStateFlow(isParticipantMode())
    val participantModeFlow: StateFlow<Boolean> = _participantModeFlow.asStateFlow()

    fun isParticipantMode(): Boolean = prefs.getBoolean(KEY_PARTICIPANT_MODE, false)
    fun setParticipantMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_PARTICIPANT_MODE, enabled) }
        _participantModeFlow.value = enabled
    }

    // Participant session id as Flow
    private val _participantSessionIdFlow = MutableStateFlow(getParticipantSessionId())
    val participantSessionIdFlow: StateFlow<Long?> = _participantSessionIdFlow.asStateFlow()

    fun getParticipantSessionId(): Long? {
        val id = prefs.getLong(KEY_PARTICIPANT_SESSION_ID, -1L)
        return if (id == -1L) null else id
    }
    fun setParticipantSessionId(sessionId: Long?) {
        if (sessionId == null) {
            prefs.edit { remove(KEY_PARTICIPANT_SESSION_ID) }
        } else {
            prefs.edit { putLong(KEY_PARTICIPANT_SESSION_ID, sessionId) }
        }
        _participantSessionIdFlow.value = sessionId
    }

    private val _participantTeamIdFlow = MutableStateFlow(getParticipantTeamId())
    val participantTeamIdFlow: StateFlow<Long?> = _participantTeamIdFlow.asStateFlow()

    fun getParticipantTeamId(): Long? {
        val id = prefs.getLong(KEY_PARTICIPANT_TEAM_ID, -1L)
        return if (id == -1L) null else id
    }
    fun setParticipantTeamId(teamId: Long?) {
        if (teamId == null) {
            prefs.edit { remove(KEY_PARTICIPANT_TEAM_ID) }
        } else {
            prefs.edit { putLong(KEY_PARTICIPANT_TEAM_ID, teamId) }
        }
        _participantTeamIdFlow.value = teamId
    }

}