package fr.centuryspine.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

interface PlayedHoleDao {
    suspend fun insert(playedHole: PlayedHole): Long

    fun getPlayedHolesForSession(sessionId: Long): Flow<List<PlayedHole>>

    fun getById(playedHoleId: Long): Flow<PlayedHole?>

    suspend fun getAll(): List<PlayedHole>

    suspend fun deletePlayedHolesBySession(sessionId: Long)

    suspend fun getPlayedHoleIdsForSession(sessionId: Long): List<Long>

    suspend fun delete(playedHole: PlayedHole)

    suspend fun deleteById(playedHoleId: Long)
}
