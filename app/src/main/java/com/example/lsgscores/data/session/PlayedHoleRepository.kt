package com.example.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

class PlayedHoleRepository(
    private val playedHoleDao: PlayedHoleDao,
    private val playedHoleScoreDao: PlayedHoleScoreDao
) {

    fun getPlayedHolesForSession(sessionId: Long): Flow<List<PlayedHole>> {
        return playedHoleDao.getPlayedHolesForSession(sessionId)
    }

    suspend fun insertPlayedHole(playedHole: PlayedHole): Long {
        return playedHoleDao.insert(playedHole)
    }
    fun getPlayedHoleById(playedHoleId: Long): Flow<PlayedHole?> {
        return playedHoleDao.getById(playedHoleId)
    }

    suspend fun deletePlayedHole(playedHoleId: Long) {
        // First delete all scores associated with this played hole
        playedHoleScoreDao.deleteScoresForPlayedHoles(listOf(playedHoleId))
        // Then delete the played hole itself
        playedHoleDao.deleteById(playedHoleId)
    }
}


