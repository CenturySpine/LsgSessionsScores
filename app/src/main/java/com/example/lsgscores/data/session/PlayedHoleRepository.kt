package com.example.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

class PlayedHoleRepository(private val playedHoleDao: PlayedHoleDao) {

    fun getPlayedHolesForSession(sessionId: Long): Flow<List<PlayedHole>> {
        return playedHoleDao.getPlayedHolesForSession(sessionId)
    }

    suspend fun insertPlayedHole(playedHole: PlayedHole): Long {
        return playedHoleDao.insert(playedHole)
    }
    fun getPlayedHoleById(playedHoleId: Long): Flow<PlayedHole?> {
        return playedHoleDao.getById(playedHoleId)
    }
}


