package fr.centuryspine.lsgscores.data.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayedHoleScoreDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : PlayedHoleScoreDao {

    override suspend fun insert(score: PlayedHoleScore): Long {
        val uid = currentUser.requireUserId()
        val inserted = supabase.postgrest["played_hole_scores"].insert(score.copy(userId = uid)) { select() }.decodeSingle<PlayedHoleScore>()
        return inserted.id
    }

    override fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>> = flow {
        val uid = currentUser.requireUserId()
        val list = supabase.postgrest["played_hole_scores"].select {
            filter { eq("playedholeid", playedHoleId); eq("user_id", uid) }
        }.decodeList<PlayedHoleScore>()
        emit(list)
    }

    override suspend fun getAll(): List<PlayedHoleScore> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["played_hole_scores"].select { filter { eq("user_id", uid) } }.decodeList()
    }

    override suspend fun deleteScoresForPlayedHoles(playedHoleIds: List<Long>) {
        // PostgREST "in" filter; do per-id deletes to keep it simple
        val uid = currentUser.requireUserId()
        for (id in playedHoleIds) {
            supabase.postgrest["played_hole_scores"].delete { filter { eq("playedholeid", id); eq("user_id", uid) } }
        }
    }
}
