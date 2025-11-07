package fr.centuryspine.lsgscores.data.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import io.github.jan.supabase.realtime.selectAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayedHoleDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : PlayedHoleDao {

    override suspend fun insert(playedHole: PlayedHole): Long {
        val uid = currentUser.requireUserId()
        val inserted = supabase.postgrest["played_holes"].insert(playedHole.copy(userId = uid)) { select() }.decodeSingle<PlayedHole>()
        return inserted.id
    }

    @OptIn(SupabaseExperimental::class)
    override fun getPlayedHolesForSession(sessionId: Long): Flow<List<PlayedHole>> =
        supabase.from("played_holes").selectAsFlow(PlayedHole::id)
            .map { rows ->
                rows.filter { it.sessionId == sessionId }
                    .sortedWith(compareBy<PlayedHole> { it.position }.thenBy { it.id })
            }

    override fun getById(playedHoleId: Long): Flow<PlayedHole?> = flow {
        // Public read to allow participants to open the score screen by playedHoleId
        val one = supabase.postgrest["played_holes"].select {
            filter { eq("id", playedHoleId) }
        }.decodeList<PlayedHole>().firstOrNull()
        emit(one)
    }

    override suspend fun getAll(): List<PlayedHole> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["played_holes"].select { filter { eq("user_id", uid) } }.decodeList()
    }

    override suspend fun deletePlayedHolesBySession(sessionId: Long) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["played_holes"].delete { filter { eq("sessionid", sessionId); eq("user_id", uid) } }
    }

    override suspend fun getPlayedHoleIdsForSession(sessionId: Long): List<Long> {
        val uid = currentUser.requireUserId()
        val rows = supabase.postgrest["played_holes"].select {
            filter { eq("sessionid", sessionId); eq("user_id", uid) }
            order("position", Order.ASCENDING)
        }.decodeList<PlayedHole>()
        return rows.map { it.id }
    }

    override suspend fun delete(playedHole: PlayedHole) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["played_holes"].delete { filter { eq("id", playedHole.id); eq("user_id", uid) } }
    }

    override suspend fun deleteById(playedHoleId: Long) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["played_holes"].delete { filter { eq("id", playedHoleId); eq("user_id", uid) } }
    }
}
