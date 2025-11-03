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
        // Legacy insert (kept for compatibility); prefer upsert()
        val inserted = supabase.postgrest["played_hole_scores"].insert(score) { select() }.decodeSingle<PlayedHoleScore>()
        return inserted.id
    }

    override suspend fun upsert(score: PlayedHoleScore): Long {
        // Last write wins on conflict (playedholeid, teamid)
        val upserted = supabase.postgrest["played_hole_scores"]
            .upsert(score, onConflict = "playedholeid,teamid") { select() }
            .decodeSingle<PlayedHoleScore>()
        return upserted.id
    }

    override fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>> = flow {
        // Public read for join participants: no owner filter
        val list = supabase.postgrest["played_hole_scores"].select {
            filter { eq("playedholeid", playedHoleId) }
        }.decodeList<PlayedHoleScore>()
        emit(list)
    }

    override suspend fun getAll(): List<PlayedHoleScore> {
        // Return all scores (administrative use). No user filter as user_id is being removed.
        return supabase.postgrest["played_hole_scores"].select().decodeList<PlayedHoleScore>()
    }

    override suspend fun deleteScoresForPlayedHoles(playedHoleIds: List<Long>) {
        // Delete by played hole ids only. RLS allows authenticated users to delete.
        for (id in playedHoleIds) {
            supabase.postgrest["played_hole_scores"].delete { filter { eq("playedholeid", id) } }
        }
    }
}
