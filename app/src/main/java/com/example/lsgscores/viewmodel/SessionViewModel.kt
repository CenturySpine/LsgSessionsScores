// viewmodel/SessionViewModel.kt

package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.media.MediaRepository
import com.example.lsgscores.data.scoring.ScoringMode
import com.example.lsgscores.data.scoring.ScoringModeRepository
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.data.session.SessionRepository
import com.example.lsgscores.data.session.SessionType
import com.example.lsgscores.data.session.TeamRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class SessionDraft(
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val sessionType: SessionType = SessionType.INDIVIDUAL,
    val scoringModeId: Int = 1,
    val comment: String? = null
)

class SessionViewModel(
    private val sessionRepository: SessionRepository,
    private val teamRepository: TeamRepository,
    private val mediaRepository: MediaRepository,
    private val scoringModeRepository: ScoringModeRepository
) : ViewModel() {

    // State for current session being created
    private val _sessionDraft = MutableStateFlow(SessionDraft())
    val sessionDraft: StateFlow<SessionDraft> = _sessionDraft.asStateFlow()

    // List of scoring modes from repository (hardcoded list)
    val scoringModes: StateFlow<List<ScoringMode>> =
        scoringModeRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update session type (individual or team)
    fun setSessionType(type: SessionType) {
        _sessionDraft.update { it.copy(sessionType = type) }
    }

    // Update scoring mode
    fun setScoringMode(id: Int) {
        _sessionDraft.update { it.copy(scoringModeId = id) }
    }

    // (Optionally, update comment if you want a comment field)
    fun setComment(comment: String?) {
        _sessionDraft.update { it.copy(comment = comment) }
    }

    // Reset session draft to defaults (for navigation/flow purposes)
    fun resetSessionDraft() {
        _sessionDraft.value = SessionDraft()
    }

    // For the first screen: build Session object but do not persist yet
    fun buildSession(): Session {
        val draft = _sessionDraft.value
        return Session(
            name = "", // To be set in further flow or extended
            dateTime = draft.dateTime,
            sessionType = draft.sessionType,
            scoringModeId = draft.scoringModeId,
            comment = draft.comment
        )
    }

    // (Later, add functions to create session/teams/medias in the database after full flow)
}
