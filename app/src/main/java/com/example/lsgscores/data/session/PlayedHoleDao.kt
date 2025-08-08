package com.example.lsgscores.data.session

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayedHoleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playedHole: PlayedHole): Long

    @Query("SELECT * FROM played_holes WHERE sessionId = :sessionId ORDER BY position ASC")
    fun getPlayedHolesForSession(sessionId: Long): Flow<List<PlayedHole>>

    // PlayedHoleDao.kt
    @Query("SELECT * FROM played_holes WHERE id = :playedHoleId")
    fun getById(playedHoleId: Long): Flow<PlayedHole?>

    @Query("DELETE FROM played_holes WHERE sessionId = :sessionId")
    suspend fun deletePlayedHolesBySession(sessionId: Long)

    @Query("SELECT id FROM played_holes WHERE sessionId = :sessionId")
    suspend fun getPlayedHoleIdsForSession(sessionId: Long): List<Long>
}
