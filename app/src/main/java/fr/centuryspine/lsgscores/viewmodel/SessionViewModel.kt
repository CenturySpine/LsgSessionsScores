package fr.centuryspine.lsgscores.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.weather.WeatherInfo
import com.example.lsgscores.data.weather.WeatherRepository
import com.example.lsgscores.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.hole.HoleRepository
import fr.centuryspine.lsgscores.data.holemode.HoleGameMode
import fr.centuryspine.lsgscores.data.holemode.HoleGameModeRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val teamRepository: TeamRepository,
    private val holeRepository: HoleRepository,
    scoringModeRepository: ScoringModeRepository,
    private val playedHoleRepository: PlayedHoleRepository,
    private val holeGameModeRepository: HoleGameModeRepository,
    private val playedHoleScoreRepository: PlayedHoleScoreRepository,
    private val gameZoneDao: GameZoneDao,
    private val weatherRepository: WeatherRepository,
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val appUserDao: fr.centuryspine.lsgscores.data.authuser.AppUserDaoSupabase
) : ViewModel() {

    // Expose helpers for join flow
    fun getSessionById(sessionId: Long): Flow<Session?> = sessionRepository.getById(sessionId)
    fun getTeamsWithPlayersForSession(sessionId: Long): Flow<List<TeamWithPlayers>> = teamRepository.getTeamsWithPlayersForSession(sessionId)
    fun forceSelectCity(cityId: Long) = appPreferences.setSelectedCityId(cityId)

    // Expose the selected city ID as a StateFlow that observes changes from AppPreferences
    val selectedCityId: StateFlow<Long?> = appPreferences.selectedCityIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // Participant mode state
    val isParticipantMode: StateFlow<Boolean> = appPreferences.participantModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val participantSessionId: StateFlow<Long?> = appPreferences.participantSessionIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val participantTeamId: StateFlow<Long?> = appPreferences.participantTeamIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setParticipantMode(enabled: Boolean) = appPreferences.setParticipantMode(enabled)
    fun setParticipantSession(sessionId: Long?) = appPreferences.setParticipantSessionId(sessionId)
    fun setParticipantTeam(teamId: Long?) = appPreferences.setParticipantTeamId(teamId)

    // Linked player: returns the playerId associated to the current authenticated user, if any
    suspend fun getLinkedPlayerIdForCurrentUser(): Long? = try {
        appUserDao.getLinkedPlayerId()
    } catch (_: Exception) { null }

    var scoringModeId: Int? = null
        private set

    // Trigger to force refresh of ongoing session after mutations (start/validate/delete)
    private val refreshCounter = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ongoingSession: StateFlow<Session?> = combine(isParticipantMode, participantSessionId, selectedCityId, refreshCounter) { isPart, partId, cityId, _ ->
        Triple(isPart, partId, cityId)
    }.flatMapLatest { (isPart, partId, cityId) ->
        when {
            isPart && partId != null -> {
                // Participant mode: actively poll the joined session by ID to reflect cross-device updates
                flow {
                    while (true) {
                        emit(sessionRepository.getById(partId).firstOrNull())
                        delay(1500)
                    }
                }
            }
            cityId != null -> {
                // Admin/owner mode: ongoing session for selected city
                sessionRepository.getOngoingSessionFlowForCity(cityId)
            }
            else -> flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    
    // Check if there's an ongoing session for the currently selected city
    val hasOngoingSessionForCurrentCity: StateFlow<Boolean> = combine(
        ongoingSession,
        selectedCityId
    ) { session, cityId ->
        session != null && cityId != null && session.cityId == cityId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Linked player id for current authenticated user (via association table)
    private val linkedPlayerIdFlow: StateFlow<Long?> = flow {
        emit(appUserDao.getLinkedPlayerId())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // For admins (non-participant mode), find the team in the ongoing session that contains the linked player
    private val adminTeamIdForOngoing: StateFlow<Long?> =
        combine(ongoingSession, linkedPlayerIdFlow) { session, pid -> session to pid }
            .flatMapLatest { (session, pid) ->
                if (session == null || pid == null) flowOf<Long?>(null) else {
                    getTeamsWithPlayersForSession(session.id).map { teams ->
                        teams.find { it.player1?.id == pid || it.player2?.id == pid }?.team?.id
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // Effective team id for the current user (unified): prefer computed id from user→link→player→team; fallback to participant preference
    val effectiveUserTeamId: StateFlow<Long?> = combine(
        participantTeamId,
        adminTeamIdForOngoing
    ) { participantId, computedId ->
        computedId ?: participantId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _error = MutableStateFlow<String?>(null)


    private val locationHelper by lazy { LocationHelper(context) }

    // State for session drafts per city
    private val _sessionDrafts = MutableStateFlow<Map<Long, SessionDraft>>(emptyMap())
    
    // Current session draft for the selected city
    val sessionDraft: StateFlow<SessionDraft> = combine(
        _sessionDrafts,
        selectedCityId
    ) { drafts, cityId ->
        if (cityId != null) {
            drafts[cityId] ?: run {
                // If no draft exists for this city, trigger initialization
                viewModelScope.launch {
                    initializeDraftForCity(cityId)
                }
                SessionDraft() // Return default for now, will be updated when initialization completes
            }
        } else {
            SessionDraft()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SessionDraft())
    val error: StateFlow<String?> = _error
    val holeGameModes: StateFlow<List<HoleGameMode>> =
        holeGameModeRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe ongoing session separately (non-blocking)
        viewModelScope.launch {
            ongoingSession.filterNotNull().collect { session ->
                scoringModeId = session.scoringModeId
            }
        }
        
        // Observe city changes and initialize drafts automatically
        viewModelScope.launch {
            selectedCityId.filterNotNull().collect { cityId ->
                if (!_sessionDrafts.value.containsKey(cityId)) {
                    initializeDraftForCity(cityId)
                }
            }
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val playedHolesWithScores: StateFlow<List<PlayedHoleDisplay>> =
        ongoingSession.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(emptyList())

            combine(
                playedHoleRepository.getPlayedHolesForSession(session.id),
                playedHoleScoreRepository.getAllRealtime(),
                teamRepository.getTeamsWithPlayersForSession(session.id),
                holeRepository.getHolesByCityId(session.cityId),
                holeGameModeRepository.getAll()
            ) { playedHoles, allScores, teamsWithPlayers, holesInCity, gameModes ->
                if (playedHoles.isEmpty()) return@combine emptyList()

                // Static maps for quick lookup
                val holeNameById = holesInCity
                    .filter { it.gameZoneId == session.gameZoneId }
                    .associateBy({ it.id }, { it.name })
                val gameModeNameById = gameModes.associateBy({ it.id }, { it.name })

                // Filter scores to only those whose playedHoleId is in our session
                val playedHoleIds = playedHoles.map { it.id }.toHashSet()
                val scoresForSession = allScores.filter { it.playedHoleId in playedHoleIds }

                // Group scores by playedHoleId for faster assembly
                val scoresByPlayedHole = scoresForSession.groupBy { it.playedHoleId }

                // Assemble display rows
                playedHoles.sortedBy { it.position }.map { ph ->
                    val scores = scoresByPlayedHole[ph.id].orEmpty()
                    val strokesByTeam: Map<Long, Int> = scores.associate { it.teamId to it.strokes }
                    val calculatedScores = computeScoresForCurrentScoringMode(strokesByTeam)

                    val teamResults = teamsWithPlayers.mapNotNull { teamWithPlayers ->
                        val teamId = teamWithPlayers.team.id
                        val strokes = strokesByTeam[teamId]
                        val calc = calculatedScores[teamId]
                        if (strokes != null && calc != null) {
                            val teamName = listOfNotNull(
                                teamWithPlayers.player1?.name,
                                teamWithPlayers.player2?.name
                            ).joinToString(" & ")
                            TeamResult(teamName, strokes, calc)
                        } else null
                    }

                    PlayedHoleDisplay(
                        playedHoleId = ph.id,
                        holeName = holeNameById[ph.holeId] ?: "Unknown Hole",
                        position = ph.position,
                        gameModeName = gameModeNameById[ph.gameModeId] ?: "Unknown Mode",
                        teamResults = teamResults
                    )
                }
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
                    else -> standings.sortedWith(
                        compareByDescending<TeamStanding> { it.totalScore }
                            .thenBy { it.totalStrokes }
                    ) // Point modes: descending by score, tie-breaker: fewer strokes
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

    // All completed sessions filtered by selected city; re-collect on refreshCounter to force refresh with Supabase-backed DAO
    @OptIn(ExperimentalCoroutinesApi::class)
    val completedSessions: StateFlow<List<Session>> =
        combine(selectedCityId, refreshCounter) { cityId, _ -> cityId }
            .flatMapLatest { cityId ->
                if (cityId != null) {
                    sessionRepository.getAll()
                        .map { sessions ->
                            sessions.filter { !it.isOngoing && it.cityId == cityId }
                                .sortedByDescending { it.dateTime }
                        }
                } else {
                    // During app startup or when city isn't chosen yet, return empty list
                    // instead of throwing and crashing collectors on other screens
                    flowOf(emptyList())
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update session type (individual or team)
    fun setSessionType(type: SessionType) {
        val currentCityId = selectedCityId.value ?: return
        _sessionDrafts.update { drafts ->
            val currentDraft = drafts[currentCityId] ?: SessionDraft()
            drafts + (currentCityId to currentDraft.copy(sessionType = type))
        }
    }

    // Update scoring mode
    fun setScoringMode(id: Int) {
        val currentCityId = selectedCityId.value ?: return
        _sessionDrafts.update { drafts ->
            val currentDraft = drafts[currentCityId] ?: SessionDraft()
            drafts + (currentCityId to currentDraft.copy(scoringModeId = id))
        }
    }

    // Update game zone
    fun setGameZoneId(id: Long) {
        val currentCityId = selectedCityId.value ?: return
        _sessionDrafts.update { drafts ->
            val currentDraft = drafts[currentCityId] ?: SessionDraft()
            drafts + (currentCityId to currentDraft.copy(gameZoneId = id))
        }
    }

    fun validateOngoingSession(onValidated: () -> Unit = {}) {
        viewModelScope.launch {
            val currentCityId = selectedCityId.value
                ?: throw Exception("No city selected")
            val ongoing = sessionRepository.getOngoingSessionForCity(currentCityId)
            if (ongoing != null) {
                val validated = ongoing.copy(
                    isOngoing = false,
                    endDateTime = LocalDateTime.now()
                )
                sessionRepository.update(validated)
                // Refresh reactive state so UI (including bottom bar) updates immediately
                refreshCounter.update { it + 1 }
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
            android.util.Log.w("SessionViewModel", "Failed to get current weather info: ${e.message}")
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

            // Check if there is already an ongoing session for the current city
            val currentCityId = selectedCityId.value
                ?: throw Exception("No city selected")
            val ongoing = sessionRepository.getOngoingSessionForCity(currentCityId)
            if (ongoing != null) {
                // Session is already ongoing for this city: block creation
                _error.value = "A session is already ongoing for this city."
                onSessionBlocked()
                return@launch
            }

            // Capture weather data (non-blocking, null if fails)
            val weatherInfo = try {
                getCurrentWeatherInfo()
            } catch (e: Exception) {
                // Log error but don't block session creation
                android.util.Log.w("SessionViewModel", "Failed to get weather info: ${e.message}")
                null
            }

            // Insert new session
            val draft = sessionDraft.value
            val session = Session(
                dateTime = draft.dateTime,
                sessionType = draft.sessionType,
                scoringModeId = draft.scoringModeId,
                gameZoneId = draft.gameZoneId, // Use gameZoneId from draft
                comment = draft.comment,
                isOngoing = true,
                weatherData = weatherInfo,
                cityId = currentCityId
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
            
            // Bump refresh trigger so OngoingSession screen and bottom bar update immediately
            refreshCounter.update { it + 1 }
            onSessionCreated(sessionId)
        }
    }

    fun savePlayedHoleScore(playedHoleId: Long, teamId: Long, strokes: Int) {
        viewModelScope.launch {
            val score = PlayedHoleScore(
                playedHoleId = playedHoleId,
                teamId = teamId,
                strokes = strokes
            )
            playedHoleScoreRepository.upsertPlayedHoleScore(score)
            // Force refresh so the UI re-queries Supabase-backed flows and displays the new/updated scores
            refreshCounter.update { it + 1 }
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
               // Bump refresh so playedHolesWithScores re-collects data via ongoingSession
               refreshCounter.update { it + 1 }
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

    fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>> {
        return playedHoleScoreRepository.getScoresForPlayedHole(playedHoleId)
    }

    fun computeScoresForCurrentScoringMode(strokesByTeam: Map<Long, Int>): Map<Long, Int> {

        val scoringId = scoringModeId ?: return emptyMap()
        val calculator: ScoringCalculator = ScoringCalculatorFactory.getCalculatorById(scoringId)
        return calculator.calculateScores(strokesByTeam)
    }

    fun deleteSessionAndAllData(session: Session, onSessionDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.deleteSessionCascade(session)
            // Refresh reactive state so UI (including bottom bar) updates immediately
            refreshCounter.update { it + 1 }
            onSessionDeleted()
        }
    }

    fun deletePlayedHole(playedHoleId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            playedHoleRepository.deletePlayedHole(playedHoleId)
            onDeleted()
        }
    }

    private suspend fun initializeDraftForCity(cityId: Long) {
        try {
            val gameZones = gameZoneDao.getGameZonesByCityId(cityId).first()
            val unknownZone = gameZones.firstOrNull { it.name == "Unknown Zone" }
            val fallbackGameZoneId = gameZones.firstOrNull()?.id ?: 1L
            
            _sessionDrafts.update { drafts ->
                if (!drafts.containsKey(cityId)) {
                    val newDraft = SessionDraft(gameZoneId = unknownZone?.id ?: fallbackGameZoneId)
                    drafts + (cityId to newDraft)
                } else {
                    drafts
                }
            }
        } catch (_: Exception) {
            // If initialization fails, use default values
            _sessionDrafts.update { drafts ->
                if (!drafts.containsKey(cityId)) {
                    drafts + (cityId to SessionDraft(gameZoneId = 1L))
                } else {
                    drafts
                }
            }
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
            val allHolesFromRepo = holeRepository.getHolesByCityId(session.cityId).first()
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


            // 7. Sort teams based on scoring mode and assign positions
            val sortedTeams = when (session.scoringModeId) {
                1 -> {
                    // Classic mode: sort by total strokes (ascending - lowest is best)
                    teamDataList
                        .sortedBy { it.totalStrokes }
                        .mapIndexed { index, teamData ->
                            teamData.copy(position = index + 1)
                        }
                }
                else -> {
                    // Point-based modes: sort by total calculated score (descending - highest is best)
                    // Tie-breaker: team with fewer total strokes ranks higher when points are equal
                    teamDataList
                        .sortedWith(
                            compareByDescending<TeamPdfData> { it.totalCalculatedScore }
                                .thenBy { it.totalStrokes }
                        )
                        .mapIndexed { index, teamData ->
                            teamData.copy(position = index + 1)
                        }
                }
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

    // region Editing Session Times
    fun updateSessionDateTimes(
        sessionId: Long,
        newStart: LocalDateTime,
        newEnd: LocalDateTime?,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                if (newStart.isAfter(now)) {
                    onResult(false, "future_start")
                    return@launch
                }
                if (newEnd != null) {
                    if (newEnd.isAfter(now)) {
                        onResult(false, "future_end")
                        return@launch
                    }
                    if (newEnd.isBefore(newStart)) {
                        onResult(false, "end_before_start")
                        return@launch
                    }
                }

                val session = sessionRepository.getById(sessionId).first()
                    ?: run {
                        onResult(false, "not_found")
                        return@launch
                    }

                // Try to fetch historical weather for the new start date/time at current device location
                val updatedWeather = try {
                    val location = locationHelper.getCurrentLocation()
                    if (location != null) {
                        val dt = newStart.atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                        weatherRepository.getHistoricalWeather(location.first, location.second, dt)
                            ?: session.weatherData
                    } else {
                        // If no location available, keep previous weather
                        session.weatherData
                    }
                } catch (_: Exception) {
                    session.weatherData // fallback to previous on any failure
                }

                val updated = session.copy(
                    dateTime = newStart,
                    endDateTime = newEnd,
                    weatherData = updatedWeather
                )
                sessionRepository.update(updated)
                // bump refresh so any observers update quickly
                refreshCounter.update { it + 1 }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    // endregion

}
