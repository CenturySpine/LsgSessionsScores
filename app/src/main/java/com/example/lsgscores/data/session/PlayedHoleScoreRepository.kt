package com.example.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

class PlayedHoleScoreRepository(private val playedHoleScoreDao: PlayedHoleScoreDao) {

    suspend fun insertPlayedHoleScore(score: PlayedHoleScore): Long {
        return playedHoleScoreDao.insert(score)
    }

    fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>> {
        return playedHoleScoreDao.getScoresForPlayedHole(playedHoleId)
    }
}