package fr.centuryspine.lsgscores.data.scoring

import kotlinx.coroutines.flow.Flow

class ScoringModeRepository(
    private val scoringModeDao: ScoringModeDao
) {
    fun getAll(): Flow<List<ScoringMode>> = scoringModeDao.getAll()

    suspend fun getAllList(): List<ScoringMode> = scoringModeDao.getAllList()

    fun getById(id: Int): Flow<ScoringMode?> = scoringModeDao.getById(id)
}
