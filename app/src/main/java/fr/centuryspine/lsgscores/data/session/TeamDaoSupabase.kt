package fr.centuryspine.lsgscores.data.session

import fr.centuryspine.lsgscores.data.player.Player
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : TeamDao {

    override suspend fun insert(team: Team): Long {
        val uid = currentUser.requireUserId()
        val inserted = supabase.postgrest["teams"].insert(team.copy(userId = uid)) { select() }.decodeSingle<Team>()
        return inserted.id
    }

    override suspend fun update(team: Team) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["teams"].update(team) {
            filter { eq("id", team.id); eq("user_id", uid) }
        }
    }

    override suspend fun delete(team: Team) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["teams"].delete {
            filter { eq("id", team.id); eq("user_id", uid) }
        }
    }

    override fun getTeamsForSession(sessionId: Long): Flow<List<Team>> =
        supabase.auth.sessionStatus.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> flow {
                    val uid = currentUser.requireUserId()
                    val list = supabase.postgrest["teams"].select {
                        filter { eq("sessionid", sessionId); eq("user_id", uid) }
                    }.decodeList<Team>()
                    emit(list)
                }

                else -> flowOf(emptyList())
            }
        }

    override suspend fun getById(id: Long): Team? {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["teams"].select {
            filter { eq("id", id); eq("user_id", uid) }
        }.decodeList<Team>().firstOrNull()
    }

    override suspend fun getAll(): List<Team> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["teams"].select { filter { eq("user_id", uid) } }.decodeList()
    }

    override suspend fun deleteTeamsForSession(sessionId: Long) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["teams"].delete { filter { eq("sessionid", sessionId); eq("user_id", uid) } }
    }

    override fun getTeamsWithPlayersForSession(sessionId: Long): Flow<List<TeamWithPlayers>> = flow {
        // Public read for join flow: fetch teams and player names without owner restriction
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
