package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.hole.HoleRepository
import fr.centuryspine.lsgscores.data.holemode.HoleGameMode
import fr.centuryspine.lsgscores.data.holemode.HoleGameModeRepository
import fr.centuryspine.lsgscores.data.media.MediaRepository
import fr.centuryspine.lsgscores.data.scoring.ScoringMode
import fr.centuryspine.lsgscores.data.scoring.ScoringModeRepository
import fr.centuryspine.lsgscores.data.session.PlayedHole
import fr.centuryspine.lsgscores.data.session.PlayedHoleRepository
import fr.centuryspine.lsgscores.data.session.PlayedHoleScore
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreRepository
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.data.session.SessionRepository
import fr.centuryspine.lsgscores.data.session.SessionType
import fr.centuryspine.lsgscores.data.session.Team
import fr.centuryspine.lsgscores.data.session.TeamRepository
import fr.centuryspine.lsgscores.data.session.TeamWithPlayers
import fr.centuryspine.lsgscores.domain.scoring.ScoringCalculator
import fr.centuryspine.lsgscores.domain.scoring.ScoringCalculatorFactory
import fr.centuryspine.lsgscores.ui.sessions.PdfScoreDisplayData
import fr.centuryspine.lsgscores.ui.sessions.SessionPdfData
import fr.centuryspine.lsgscores.ui.sessions.TeamPdfData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import android.content.Context
import com.example.lsgscores.data.weather.WeatherRepository
import com.example.lsgscores.data.weather.WeatherInfo
import com.example.lsgscores.utils.LocationHelper
import dagger.hilt.android.qualifiers.ApplicationContext


@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val teamRepository: TeamRepository,
    private val holeRepository: HoleRepository,
    private val mediaRepository: MediaRepository,
    scoringModeRepository: ScoringModeRepository,
    private val playedHoleRepository: PlayedHoleRepository,
    private val holeGameModeRepository: HoleGameModeRepository,
    private val playedHoleScoreRepository: PlayedHoleScoreRepository,
    private val gameZoneDao: GameZoneDao,
    private val weatherRepository: WeatherRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var scoringModeId: Int? = null
        private set

    val ongoingSession: StateFlow<Session?> =
        sessionRepository.getOngoingSessionFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _error = MutableStateFlow<String?>(null)


    private val locationHelper by lazy { LocationHelper(context) }

    // State for current session being created
    private val _sessionDraft = MutableStateFlow(SessionDraft())
    val sessionDraft: StateFlow<SessionDraft> = _sessionDraft.asStateFlow()
    val error: StateFlow<String?> = _error
    val holeGameModes: StateFlow<List<HoleGameMode>> =
        holeGameModeRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Initialize sessionDraft with a default gameZoneId (e.g., the 'Unknown Zone')
            val unknownZone =
                gameZoneDao.getAllGameZones().first().firstOrNull { it.name == "Zone Inconnue" }
            _sessionDraft.update { it.copy(gameZoneId = unknownZone?.id ?: 1L) }

            // Existing logic for ongoing session
            ongoingSession.filterNotNull().collect { session ->
                scoringModeId = session.scoringModeId
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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
                                        holeGameModeRepository.getById(playedHole.gameModeId),
                                        playedHoleScoreRepository.getScoresForPlayedHole(playedHole.id),
                                        teamRepository.getTeamsWithPlayersForSession(session.id)
                                    ) { hole, gameMode, scores, teamsWithPlayers ->
                                        val strokesByTeam =
                                            scores.associate { it.teamId to it.strokes }
                                        val calculatedScores =
                                            computeScoresForCurrentScoringMode(strokesByTeam)

                                        val teamResults =
                                            teamsWithPlayers.mapNotNull { teamWithPlayers ->
                                                val strokes = strokesByTeam[teamWithPlayers.team.id]
                                                val calculatedScore =
                                                    calculatedScores[teamWithPlayers.team.id]
                                                if (strokes != null && calculatedScore != null) {
                                                    val teamName = listOfNotNull(
                                                        teamWithPlayers.player1?.name,
                                                        teamWithPlayers.player2?.name
                                                    ).joinToString(" & ")
                                                    TeamResult(teamName, strokes, calculatedScore)
                                                } else null
                                            }

                                        PlayedHoleDisplay(
                                            playedHoleId = playedHole.id,
                                            holeName = hole?.name ?: "Unknown Hole",
                                            position = playedHole.position,
                                            gameModeName = gameMode?.name ?: "Unknown Mode",
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentScoringModeInfo: StateFlow<ScoringMode?> =
        ongoingSession.flatMapLatest { session ->
            if (session != null) {
                scoringModeRepository.getById(session.scoringModeId)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val teamStandings: StateFlow<List<TeamStanding>> =
        playedHolesWithScores.map { playedHoles ->
            if (playedHoles.isEmpty()) {
                emptyList()
            } else {
                // Aggregate totals by team
                val teamTotals =
                    mutableMapOf<String, Pair<Int, Int>>() // teamName to (totalStrokes, totalScore)

                playedHoles.forEach { playedHole ->
                    playedHole.teamResults.forEach { teamResult ->
                        val currentStrokes = teamTotals[teamResult.teamName]?.first ?: 0
                        val currentScore = teamTotals[teamResult.teamName]?.second ?: 0
                        teamTotals[teamResult.teamName] = Pair(
                            currentStrokes + teamResult.strokes,
                            currentScore + teamResult.calculatedScore
                        )
                    }
                }

                // Convert to list and sort according to scoring mode
                val standings = teamTotals.map { (teamName, totals) ->
                    TeamStanding(
                        teamName = teamName,
                        totalStrokes = totals.first,
                        totalScore = totals.second,
                        position = 0 // Will be set after sorting
                    )
                }

                // Sort based on current scoring mode
                val sortedStandings = when (scoringModeId) {
                    1 -> standings.sortedBy { it.totalStrokes } // Classic mode: ascending by strokes
                    else -> standings.sortedByDescending { it.totalScore } // Point modes: descending by score
                }

                // Assign positions
                sortedStandings.mapIndexed { index, standing ->
                    standing.copy(position = index + 1)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of scoring modes from repository (hardcoded list)
    val scoringModes: StateFlow<List<ScoringMode>> =
        scoringModeRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Add this property to get all completed sessions
    val completedSessions: StateFlow<List<Session>> =
        sessionRepository.getAll()
            .map { sessions ->
                sessions.filter { !it.isOngoing }
                    .sortedByDescending { it.dateTime }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update session type (individual or team)
    fun setSessionType(type: SessionType) {
        _sessionDraft.update { it.copy(sessionType = type) }
    }

    // Update scoring mode
    fun setScoringMode(id: Int) {
        _sessionDraft.update { it.copy(scoringModeId = id) }
    }

    // Update game zone
    fun setGameZoneId(id: Long) {
        _sessionDraft.update { it.copy(gameZoneId = id) }
    }

    fun validateOngoingSession(onValidated: () -> Unit = {}) {
        viewModelScope.launch {
            val ongoing = sessionRepository.getOngoingSession()
            if (ongoing != null) {
                val validated = ongoing.copy(
                    isOngoing = false,
                    endDateTime = LocalDateTime.now()
                )
                sessionRepository.update(validated)
                onValidated()
            }
        }
    }

    suspend fun getCurrentWeatherInfo(): WeatherInfo? {
        return try {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                weatherRepository.getCurrentWeather(location.first, location.second)
            } else {
                null
            }
        } catch (e: Exception) {
            null
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
                dateTime = draft.dateTime,
                sessionType = draft.sessionType,
                scoringModeId = draft.scoringModeId,
                gameZoneId = draft.gameZoneId, // Use gameZoneId from draft
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

    @OptIn(ExperimentalCoroutinesApi::class)
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

    @OptIn(ExperimentalCoroutinesApi::class)
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

    fun deletePlayedHole(playedHoleId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            playedHoleRepository.deletePlayedHole(playedHoleId)
            onDeleted()
        }
    }

    fun loadSessionPdfData(session: Session): Flow<SessionPdfData> {
        return flow {
            // 1. Get GameZone for the session
            val gameZone = gameZoneDao.getGameZoneById(session.gameZoneId)

            // 2. Get Teams with Players
            val teamsWithPlayers = teamRepository.getTeamsWithPlayersForSession(session.id).first()

            // 3. Get PlayedHoles
            val playedHoles = playedHoleRepository.getPlayedHolesForSession(session.id).first()

            // 4. Get HolesDetails
            val allHolesFromRepo = holeRepository.getAllHoles().first()
            val holesDetailsMap = mutableMapOf<Long, Hole>()
            playedHoles.forEach { playedHole ->
                allHolesFromRepo.find { it.id == playedHole.holeId }?.let { holeDetail ->
                    holesDetailsMap[playedHole.holeId] = holeDetail
                }
            }

            // 5. Get HoleGameModes
            val allGameModes = holeGameModeRepository.getAll().first()
            val holeGameModesMap = allGameModes.associate { it.id.toLong() to it.name }

            // 6. Build team data with scores
            val teamDataList = mutableListOf<TeamPdfData>()

            teamsWithPlayers.forEach { teamWithPlayers ->
                val team = teamWithPlayers.team
                val player1Name = teamWithPlayers.player1?.name ?: ""
                val player2Name = teamWithPlayers.player2?.name?.let { " & $it" } ?: ""
                val teamName = "$player1Name$player2Name".takeIf { it.isNotBlank() }
                    ?: "Team ${team.id}"

                // Collect scores for this team
                val holeScoresMap = mutableMapOf<Long, PdfScoreDisplayData>()
                var totalStrokes = 0
                var totalCalculatedScore = 0

                playedHoles.forEach { playedHole ->
                    val actualScoresForHole = playedHoleScoreRepository
                        .getScoresForPlayedHole(playedHole.id).first()
                    val strokesForHoleByTeam = actualScoresForHole
                        .associate { it.teamId to it.strokes }

                    // Calculate scores using the scoring mode
                    val calculator = ScoringCalculatorFactory
                        .getCalculatorById(session.scoringModeId)
                    val calculatedScoresByTeam = calculator
                        .calculateScores(strokesForHoleByTeam)

                    // Find this team's score
                    val teamScore = actualScoresForHole.find { it.teamId == team.id }
                    if (teamScore != null) {
                        val calculatedScore = calculatedScoresByTeam[team.id] ?: 0
                        holeScoresMap[playedHole.id] = PdfScoreDisplayData(
                            strokes = teamScore.strokes,
                            calculatedScore = calculatedScore
                        )
                        totalStrokes += teamScore.strokes
                        totalCalculatedScore += calculatedScore
                    }
                }

                teamDataList.add(
                    TeamPdfData(
                        teamId = team.id,
                        teamName = teamName,
                        holeScores = holeScoresMap,
                        totalStrokes = totalStrokes,
                        totalCalculatedScore = totalCalculatedScore
                    )
                )
            }

            // 7. Sort teams by total calculated score (descending) and assign positions
            val sortedTeams = teamDataList
                .sortedByDescending { it.totalCalculatedScore }
                .mapIndexed { index, teamData ->
                    teamData.copy(position = index + 1)
                }

            emit(
                SessionPdfData(
                    session = session,
                    teams = sortedTeams,
                    playedHoles = playedHoles,
                    holesDetails = holesDetailsMap.toMap(),
                    gameZone = gameZone,
                    holeGameModes = holeGameModesMap
                )
            )
        }
    }
}
