// viewmodel/SessionViewModel.kt

package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.hole.HoleRepository
import com.example.lsgscores.data.holemode.HoleGameMode
import com.example.lsgscores.data.holemode.HoleGameModeRepository
import com.example.lsgscores.data.media.MediaRepository
import com.example.lsgscores.data.scoring.ScoringMode
import com.example.lsgscores.data.scoring.ScoringModeRepository
import com.example.lsgscores.data.session.PlayedHole
import com.example.lsgscores.data.session.PlayedHoleRepository
import com.example.lsgscores.data.session.PlayedHoleScore
import com.example.lsgscores.data.session.PlayedHoleScoreRepository
import com.example.lsgscores.data.session.Session
import com.example.lsgscores.data.session.SessionRepository
import com.example.lsgscores.data.session.SessionType
import com.example.lsgscores.data.session.Team
import com.example.lsgscores.data.session.TeamRepository
import com.example.lsgscores.data.session.TeamWithPlayers
import com.example.lsgscores.domain.scoring.ScoringCalculator
import com.example.lsgscores.domain.scoring.ScoringCalculatorFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class PlayedHoleDisplay(
    val holeName: String,
    val position: Int,
    val teamResults: List<TeamResult>
)

data class TeamResult(
    val teamName: String,
    val strokes: Int,
    val calculatedScore: Int
)
data class SessionDraft(
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val sessionType: SessionType = SessionType.INDIVIDUAL,
    val scoringModeId: Int = 1,
    val comment: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val teamRepository: TeamRepository,
    private val holeRepository: HoleRepository,
    private val mediaRepository: MediaRepository,
    scoringModeRepository: ScoringModeRepository,
    private val playedHoleRepository: PlayedHoleRepository,
    holeGameModeRepository: HoleGameModeRepository,
    private val playedHoleScoreRepository: PlayedHoleScoreRepository
) : ViewModel() {

    var scoringModeId: Int? = null
        private set

    val ongoingSession: StateFlow<Session?> =
        sessionRepository.getOngoingSessionFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    init {
        viewModelScope.launch {
            ongoingSession.filterNotNull().collect { session ->
                scoringModeId = session.scoringModeId
            }
        }
    }

    private val _error = MutableStateFlow<String?>(null)

    // State for current session being created
    private val _sessionDraft = MutableStateFlow(SessionDraft())
    val sessionDraft: StateFlow<SessionDraft> = _sessionDraft.asStateFlow()
    val error: StateFlow<String?> = _error
    val holeGameModes: StateFlow<List<HoleGameMode>> =
        holeGameModeRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playedHolesWithScores: StateFlow<List<PlayedHoleDisplay>> =
        ongoingSession.flatMapLatest { session ->
            if (session != null) {
                playedHoleRepository.getPlayedHolesForSession(session.id)
                    .flatMapLatest { playedHoles ->
                        if (playedHoles.isEmpty()) {
                            flowOf(emptyList())
                        } else {
                            combine(
                                playedHoles.map { playedHole ->
                                    combine(
                                        holeRepository.getAllHoles().map { holes ->
                                            holes.find { it.id == playedHole.holeId }
                                        },
                                        playedHoleScoreRepository.getScoresForPlayedHole(playedHole.id),
                                        teamRepository.getTeamsWithPlayersForSession(session.id)
                                    ) { hole, scores, teamsWithPlayers ->
                                        val strokesByTeam = scores.associate { it.teamId to it.strokes }
                                        val calculatedScores = computeScoresForCurrentScoringMode(strokesByTeam)

                                        val teamResults = teamsWithPlayers.mapNotNull { teamWithPlayers ->
                                            val strokes = strokesByTeam[teamWithPlayers.team.id]
                                            val calculatedScore = calculatedScores[teamWithPlayers.team.id]
                                            if (strokes != null && calculatedScore != null) {
                                                val teamName = listOfNotNull(
                                                    teamWithPlayers.player1?.name,
                                                    teamWithPlayers.player2?.name
                                                ).joinToString(" & ")
                                                TeamResult(teamName, strokes, calculatedScore)
                                            } else null
                                        }

                                        PlayedHoleDisplay(
                                            holeName = hole?.name ?: "Unknown Hole",
                                            position = playedHole.position,
                                            teamResults = teamResults
                                        )
                                    }
                                }
                            ) { it.toList().sortedBy { display -> display.position } }
                        }
                    }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /**
     * Validate the ongoing session: just remove the ongoing flag.
     */
    fun validateOngoingSession(onValidated: () -> Unit = {}) {
        viewModelScope.launch {
            val ongoing = sessionRepository.getOngoingSession()
            if (ongoing != null) {
                val validated = ongoing.copy(isOngoing = false)
                sessionRepository.update(validated)
                onValidated()
            }
        }
    }

    /**
     * Delete the ongoing session and all its teams and related data.
     */
    fun deleteOngoingSession(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val ongoing = sessionRepository.getOngoingSession()
            if (ongoing != null) {
                // Delete teams associated with this session (if cascade not handled by Room)
                teamRepository.deleteTeamsForSession(ongoing.id)
                sessionRepository.delete(ongoing)
                onDeleted()
            }
        }
    }

    /**
     * Creates and persists a new ongoing session and its teams.
     * Ensures only one session is ongoing at a time.
     *
     * @param teams List of pairs/lists of user IDs representing the teams.
     * @param onSessionCreated Callback with the session ID after creation.
     */
    fun startSessionWithTeams(
        teams: List<List<Long>>,
        onSessionCreated: (Long) -> Unit = {},
        onSessionBlocked: () -> Unit = {}
    ) {
        viewModelScope.launch {
// Check if there is already an ongoing session
            val ongoing = sessionRepository.getOngoingSession()
            if (ongoing != null) {
                // Session is already ongoing: block creation
                _error.value = "A session is already ongoing."
                onSessionBlocked()
                return@launch
            }

            // Insert new session
            val draft = _sessionDraft.value
            val session = Session(
                name = "", // Set a name if needed
                dateTime = draft.dateTime,
                sessionType = draft.sessionType,
                scoringModeId = draft.scoringModeId,
                comment = draft.comment,
                isOngoing = true
            )
            val sessionId = sessionRepository.insert(session)
            scoringModeId = draft.scoringModeId
            // Insert teams for this session
            teams.forEach { playerIds ->
                val player1Id = playerIds.getOrNull(0)
                val player2Id = playerIds.getOrNull(1)
                if (player1Id != null) {
                    teamRepository.insert(
                        Team(
                            sessionId = sessionId,
                            player1Id = player1Id,
                            player2Id = player2Id
                        )
                    )
                }
            }

            onSessionCreated(sessionId)
        }
    }

    fun getTeamsForPlayedHole(playedHoleId: Long): Flow<List<Team>> {
        return playedHoleRepository.getPlayedHoleById(playedHoleId)
            .flatMapLatest { playedHole ->
                if (playedHole != null) {
                    teamRepository.getTeamsForSession(playedHole.sessionId)
                } else {
                    flowOf(emptyList())
                }
            }
    }

    fun savePlayedHoleScore(playedHoleId: Long, teamId: Long, strokes: Int) {
        viewModelScope.launch {
            val score = PlayedHoleScore(
                playedHoleId = playedHoleId,
                teamId = teamId,
                strokes = strokes
            )
            playedHoleScoreRepository.insertPlayedHoleScore(score)
        }
    }

    /**
     * Adds a played hole to the ongoing session.
     * The new hole is added at the end of the session (next position).
     */
    fun addPlayedHole(holeId: Long, gameModeId: Int, onPlayedHoleCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val session = ongoingSession.value
            if (session != null) {
                val playedHoles = playedHoleRepository.getPlayedHolesForSession(session.id)
                    .firstOrNull() ?: emptyList()
                val nextPosition = playedHoles.size + 1
                val playedHole = PlayedHole(
                    sessionId = session.id,
                    holeId = holeId,
                    gameModeId = gameModeId,
                    position = nextPosition
                )
                val playedHoleId = playedHoleRepository.insertPlayedHole(playedHole)
                onPlayedHoleCreated(playedHoleId)
            }
        }
    }

    fun getTeamsWithPlayersForPlayedHole(playedHoleId: Long): Flow<List<TeamWithPlayers>> {
        return playedHoleRepository.getPlayedHoleById(playedHoleId)
            .flatMapLatest { playedHole ->
                if (playedHole != null) {
                    teamRepository.getTeamsWithPlayersForSession(playedHole.sessionId)
                } else {
                    flowOf(emptyList())
                }
            }
    }

    fun computeScoresForCurrentScoringMode(strokesByTeam: Map<Long, Int>): Map<Long, Int> {
        // Vérifie que scoringModeId est bien défini
        val scoringId = scoringModeId ?: return emptyMap()
        val calculator: ScoringCalculator = ScoringCalculatorFactory.getCalculatorById(scoringId)
        return calculator.calculateScores(strokesByTeam)
    }

    fun deleteSessionAndAllData(session: Session, onSessionDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.deleteSessionCascade(session)
            onSessionDeleted()
        }
    }

}
