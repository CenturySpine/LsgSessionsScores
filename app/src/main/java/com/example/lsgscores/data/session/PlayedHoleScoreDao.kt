package com.example.lsgscores.data.session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayedHoleScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: PlayedHoleScore): Long

    @Query("SELECT * FROM played_hole_scores WHERE playedHoleId = :playedHoleId")
    fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>>
}