package fr.centuryspine.lsgscores.data.scoring


import androidx.room.Dao
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

@Dao
interface ScoringModeDao {

    @Query("SELECT * FROM scoring_modes")
    fun getAll(): Flow<List<ScoringMode>>

    @Query("SELECT * FROM scoring_modes WHERE id = :id")
    fun getById(id: Int): Flow<ScoringMode?>
}
