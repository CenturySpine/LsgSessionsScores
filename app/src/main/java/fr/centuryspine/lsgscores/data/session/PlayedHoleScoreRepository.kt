package fr.centuryspine.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

class PlayedHoleScoreRepository(private val playedHoleScoreDao: PlayedHoleScoreDao) {

    suspend fun insertPlayedHoleScore(score: PlayedHoleScore): Long {
        return playedHoleScoreDao.insert(score)
    }

    suspend fun upsertPlayedHoleScore(score: PlayedHoleScore): Long {
        return playedHoleScoreDao.upsert(score)
    }

    fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>> {
        return playedHoleScoreDao.getScoresForPlayedHole(playedHoleId)
    }

    fun getAllRealtime(): Flow<List<PlayedHoleScore>> {
        return playedHoleScoreDao.getAllRealtime()
    }
}