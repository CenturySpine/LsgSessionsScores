package fr.centuryspine.lsgscores.data.session

import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val sessionDao: SessionDao,
    private val teamDao: TeamDao,
    private val playedHoleDao: PlayedHoleDao,
    private val playedHoleScoreDao: PlayedHoleScoreDao,
    private val gameZoneDao: GameZoneDao
) {

    fun getAll(): Flow<List<Session>> = sessionDao.getAll()

    fun getById(id: Long): Flow<Session?> = sessionDao.getById(id)

//    fun getRealtimeSessions(): Flow<List<Session>> = sessionDao.getRealtimeSessions()

    suspend fun insert(session: Session): Long {
        // Validate that GameZone exists
        val gameZone = gameZoneDao.getGameZoneById(session.gameZoneId)
        if (gameZone == null) {
            throw IllegalArgumentException("GameZone with id ${session.gameZoneId} does not exist")
        }
        return sessionDao.insert(session)
    }

    suspend fun update(session: Session) {
        // Validate that GameZone exists
        val gameZone = gameZoneDao.getGameZoneById(session.gameZoneId)
        if (gameZone == null) {
            throw IllegalArgumentException("GameZone with id ${session.gameZoneId} does not exist")
        }
        sessionDao.update(session)
    }

    suspend fun delete(session: Session) = sessionDao.delete(session)

    suspend fun getOngoingSessionForCity(cityId: Long): Session? = sessionDao.getOngoingSessionForCity(cityId)

    fun getOngoingSessionFlowForCity(cityId: Long): Flow<Session?> = sessionDao.getOngoingSessionFlowForCity(cityId)

    val realtimeSessionFlow: Flow<List<Session>> = sessionDao.realtimeSessionFlow

    suspend fun deleteSessionCascade(session: Session) {
        // 1. Récupérer tous les playedHoleIds de la session
        val playedHoleIds = playedHoleDao.getPlayedHoleIdsForSession(session.id)
        // 2. Supprimer tous les scores associés à ces playedHoleIds
        if (playedHoleIds.isNotEmpty()) {
            playedHoleScoreDao.deleteScoresForPlayedHoles(playedHoleIds)
        }
        // 3. Supprimer les playedHoles de la session
        playedHoleDao.deletePlayedHolesBySession(session.id)
        // 4. Supprimer les équipes de la session
        teamDao.deleteTeamsBySession(session.id)
        // 5. Supprimer la session elle-même
        sessionDao.delete(session)
    }
}
