package com.example.lsgscores.data.holemode

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HoleGameModeDao {

    @Query("SELECT * FROM hole_game_modes")
    fun getAll(): Flow<List<HoleGameMode>>

    @Query("SELECT * FROM hole_game_modes WHERE id = :id")
    fun getById(id: Int): Flow<HoleGameMode?>
}