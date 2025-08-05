// data/session/TeamRepository.kt

package com.example.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

class TeamRepository(private val teamDao: TeamDao) {

    suspend fun insert(team: Team): Long = teamDao.insert(team)

    suspend fun update(team: Team) = teamDao.update(team)

    suspend fun delete(team: Team) = teamDao.delete(team)

    fun getTeamsForSession(sessionId: Int): Flow<List<Team>> = teamDao.getTeamsForSession(sessionId)

    suspend fun getById(id: Int): Team? = teamDao.getById(id)

    /**
     * Delete all teams for a given session.
     */
    suspend fun deleteTeamsForSession(sessionId: Long) {
        teamDao.deleteTeamsForSession(sessionId)
    }
}
