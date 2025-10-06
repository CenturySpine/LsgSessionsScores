package fr.centuryspine.lsgscores.data.scoring

import kotlinx.coroutines.flow.Flow

interface ScoringModeDao {
    fun getAll(): Flow<List<ScoringMode>>

    suspend fun getAllList(): List<ScoringMode>

    fun getById(id: Int): Flow<ScoringMode?>
}
