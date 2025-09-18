package fr.centuryspine.lsgscores.data.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayedHoleDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : PlayedHoleDao {

    override suspend fun insert(playedHole: PlayedHole): Long {
        val inserted = supabase.postgrest["played_holes"].insert(playedHole).decodeSingle<PlayedHole>()
        return inserted.id
    }

    override fun getPlayedHolesForSession(sessionId: Long): Flow<List<PlayedHole>> = flow {
        val list = supabase.postgrest["played_holes"].select {
            filter { eq("sessionid", sessionId) }
            order("position", Order.ASCENDING)
        }.decodeList<PlayedHole>()
        emit(list)
    }

    override fun getById(playedHoleId: Long): Flow<PlayedHole?> = flow {
        val one = supabase.postgrest["played_holes"].select { filter { eq("id", playedHoleId) } }.decodeList<PlayedHole>().firstOrNull()
        emit(one)
    }

    override suspend fun getAll(): List<PlayedHole> {
        return supabase.postgrest["played_holes"].select().decodeList()
    }

    override suspend fun deletePlayedHolesBySession(sessionId: Long) {
        supabase.postgrest["played_holes"].delete { filter { eq("sessionid", sessionId) } }
    }

    override suspend fun getPlayedHoleIdsForSession(sessionId: Long): List<Long> {
        val rows = supabase.postgrest["played_holes"].select { filter { eq("sessionid", sessionId) }; order("position", Order.ASCENDING) }.decodeList<PlayedHole>()
        return rows.map { it.id }
    }

    override suspend fun delete(playedHole: PlayedHole) {
        supabase.postgrest["played_holes"].delete { filter { eq("id", playedHole.id) } }
    }

    override suspend fun deleteById(playedHoleId: Long) {
        supabase.postgrest["played_holes"].delete { filter { eq("id", playedHoleId) } }
    }
}
