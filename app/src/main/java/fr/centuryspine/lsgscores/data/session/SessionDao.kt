package fr.centuryspine.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

interface SessionDao {
    fun getAll(): Flow<List<Session>>

    suspend fun getAllList(): List<Session>

    fun getById(id: Long): Flow<Session?>

    suspend fun insert(session: Session): Long

    suspend fun update(session: Session)

    suspend fun delete(session: Session)

    suspend fun getOngoingSession(): Session?

    suspend fun getOngoingSessionForCity(cityId: Long): Session?

    suspend fun clearOngoingSessions()

    suspend fun clearOngoingSessionsForCity(cityId: Long)

    fun getOngoingSessionFlow(): Flow<Session?>

    fun getOngoingSessionFlowForCity(cityId: Long): Flow<Session?>

    suspend fun getSessionsByGameZoneId(gameZoneId: Long): List<Session>

    // Realtime stream of sessions (raw table stream); consumers should filter as needed
//    fun getRealtimeSessions(): Flow<List<Session>>

    val realtimeSessionFlow: Flow<List<Session>>
}
