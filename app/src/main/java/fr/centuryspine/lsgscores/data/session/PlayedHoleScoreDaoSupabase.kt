package fr.centuryspine.lsgscores.data.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayedHoleScoreDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : PlayedHoleScoreDao {

    override suspend fun insert(score: PlayedHoleScore): Long {
        val inserted = supabase.postgrest["played_hole_scores"].insert(score) { select() }.decodeSingle<PlayedHoleScore>()
        return inserted.id
    }

    override fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>> = flow {
        val list = supabase.postgrest["played_hole_scores"].select {
            filter { eq("playedholeid", playedHoleId) }
        }.decodeList<PlayedHoleScore>()
        emit(list)
    }

    override suspend fun getAll(): List<PlayedHoleScore> {
        return supabase.postgrest["played_hole_scores"].select().decodeList()
    }

    override suspend fun deleteScoresForPlayedHoles(playedHoleIds: List<Long>) {
        // PostgREST "in" filter; do per-id deletes to keep it simple
        for (id in playedHoleIds) {
            supabase.postgrest["played_hole_scores"].delete { filter { eq("playedholeid", id) } }
        }
    }
}
