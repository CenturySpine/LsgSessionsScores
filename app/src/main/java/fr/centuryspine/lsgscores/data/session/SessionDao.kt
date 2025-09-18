package fr.centuryspine.lsgscores.data.session


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllList(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getById(id: Int): Flow<Session?>

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("SELECT * FROM sessions WHERE isOngoing = 1 LIMIT 1")
    suspend fun getOngoingSession(): Session?

    @Query("SELECT * FROM sessions WHERE isOngoing = 1 AND cityId = :cityId LIMIT 1")
    suspend fun getOngoingSessionForCity(cityId: Long): Session?

    @Query("UPDATE sessions SET isOngoing = 0 WHERE isOngoing = 1")
    suspend fun clearOngoingSessions()

    @Query("UPDATE sessions SET isOngoing = 0 WHERE isOngoing = 1 AND cityId = :cityId")
    suspend fun clearOngoingSessionsForCity(cityId: Long)

    @Query("SELECT * FROM sessions WHERE isOngoing = 1 LIMIT 1")
    fun getOngoingSessionFlow(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE isOngoing = 1 AND cityId = :cityId LIMIT 1")
    fun getOngoingSessionFlowForCity(cityId: Long): Flow<Session?>
    @Query("SELECT * FROM sessions WHERE gameZoneId = :gameZoneId")
    suspend fun getSessionsByGameZoneId(gameZoneId: Long): List<Session>
}
