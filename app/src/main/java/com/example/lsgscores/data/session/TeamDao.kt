// data/session/TeamDao.kt

package com.example.lsgscores.data.session

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Insert
    suspend fun insert(team: Team): Long

    @Update
    suspend fun update(team: Team)

    @Delete
    suspend fun delete(team: Team)

    @Query("SELECT * FROM teams WHERE sessionId = :sessionId")
    fun getTeamsForSession(sessionId: Long): Flow<List<Team>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getById(id: Long): Team?

    /**
     * Delete all teams for a given session.
     */
    @Query("DELETE FROM teams WHERE sessionId = :sessionId")
    suspend fun deleteTeamsForSession(sessionId: Long)

    @Transaction
    @Query("SELECT * FROM teams WHERE sessionId = :sessionId")
    fun getTeamsWithPlayersForSession(sessionId: Long): Flow<List<TeamWithPlayers>>

    @Query("DELETE FROM teams WHERE sessionId = :sessionId")
    suspend fun deleteTeamsBySession(sessionId: Long)
}
