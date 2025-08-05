package com.example.lsgscores.data.session
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {

    fun getAll(): Flow<List<Session>> = sessionDao.getAll()

    fun getById(id: Int): Flow<Session?> = sessionDao.getById(id)

    suspend fun insert(session: Session): Long = sessionDao.insert(session)

    suspend fun update(session: Session) = sessionDao.update(session)

    suspend fun delete(session: Session) = sessionDao.delete(session)

    suspend fun getOngoingSession(): Session? = sessionDao.getOngoingSession()

    suspend fun clearOngoingSessions() = sessionDao.clearOngoingSessions()

    fun getOngoingSessionFlow(): Flow<Session?> = sessionDao.getOngoingSessionFlow()
}
