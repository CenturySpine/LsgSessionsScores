package fr.centuryspine.lsgscores.data.session

import fr.centuryspine.lsgscores.data.player.Player
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : TeamDao {

    override suspend fun insert(team: Team): Long {
        val inserted = supabase.postgrest["teams"].insert(team) { select() }.decodeSingle<Team>()
        return inserted.id
    }

    override suspend fun update(team: Team) {
        supabase.postgrest["teams"].update(team) {
            filter { eq("id", team.id) }
        }
    }

    override suspend fun delete(team: Team) {
        supabase.postgrest["teams"].delete {
            filter { eq("id", team.id) }
        }
    }

    override fun getTeamsForSession(sessionId: Long): Flow<List<Team>> = flow {
        val list = supabase.postgrest["teams"].select {
            filter { eq("sessionid", sessionId) }
        }.decodeList<Team>()
        emit(list)
    }

    override suspend fun getById(id: Long): Team? {
        return supabase.postgrest["teams"].select {
            filter { eq("id", id) }
        }.decodeList<Team>().firstOrNull()
    }

    override suspend fun getAll(): List<Team> {
        return supabase.postgrest["teams"].select().decodeList()
    }

    override suspend fun deleteTeamsForSession(sessionId: Long) {
        supabase.postgrest["teams"].delete { filter { eq("sessionid", sessionId) } }
    }

    override fun getTeamsWithPlayersForSession(sessionId: Long): Flow<List<TeamWithPlayers>> = flow {
        val teams = supabase.postgrest["teams"].select { filter { eq("sessionid", sessionId) } }.decodeList<Team>()
        val result = mutableListOf<TeamWithPlayers>()
        for (t in teams) {
            val p1 = t.player1Id.let {
                supabase.postgrest["players"].select { filter { eq("id", it) } }.decodeList<Player>().firstOrNull()
            }
            val p2 = t.player2Id?.let {
                supabase.postgrest["players"].select { filter { eq("id", it) } }.decodeList<Player>().firstOrNull()
            }
            result += TeamWithPlayers(team = t, player1 = p1, player2 = p2)
        }
        emit(result)
    }

    override suspend fun deleteTeamsBySession(sessionId: Long) {
        // Same as deleteTeamsForSession
        deleteTeamsForSession(sessionId)
    }
}
