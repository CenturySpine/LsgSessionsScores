// data/session/TeamDao.kt

package fr.centuryspine.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

interface TeamDao {

    suspend fun insert(team: Team): Long

    suspend fun update(team: Team)

    suspend fun delete(team: Team)

    fun getTeamsForSession(sessionId: Long): Flow<List<Team>>

    suspend fun getById(id: Long): Team?

    suspend fun getAll(): List<Team>

    /**
     * Delete all teams for a given session.
     */
    suspend fun deleteTeamsForSession(sessionId: Long)

    fun getTeamsWithPlayersForSession(sessionId: Long): Flow<List<TeamWithPlayers>>

    suspend fun deleteTeamsBySession(sessionId: Long)
}
