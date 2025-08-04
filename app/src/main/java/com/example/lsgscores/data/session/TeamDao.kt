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
    fun getTeamsForSession(sessionId: Int): Flow<List<Team>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getById(id: Int): Team?
}
