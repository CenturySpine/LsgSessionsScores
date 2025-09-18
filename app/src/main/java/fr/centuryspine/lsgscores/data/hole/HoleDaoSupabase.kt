package fr.centuryspine.lsgscores.data.hole

import fr.centuryspine.lsgscores.data.gamezone.GameZone
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoleDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : HoleDao {

    override fun getHolesByCityId(cityId: Long): Flow<List<Hole>> = flow {
        // Fetch game zones for this city, then fetch holes for each zone and merge
        val zones = supabase.postgrest["game_zones"].select {
            filter { eq("cityid", cityId) }
            order("name", Order.ASCENDING)
        }.decodeList<GameZone>()
        val result = mutableListOf<Hole>()
        for (gz in zones) {
            val holes = supabase.postgrest["holes"].select {
                filter { eq("gamezoneid", gz.id) }
                order("name", Order.ASCENDING)
            }.decodeList<Hole>()
            result += holes
        }
        emit(result)
    }

    override suspend fun getAll(): List<Hole> {
        return supabase.postgrest["holes"].select().decodeList<Hole>()
    }

    override suspend fun insert(hole: Hole): Long {
        val inserted = supabase.postgrest["holes"].insert(hole).decodeSingle<Hole>()
        return inserted.id
    }

    override suspend fun update(hole: Hole) {
        supabase.postgrest["holes"].update(hole) {
            filter { eq("id", hole.id) }
        }
    }

    override suspend fun delete(hole: Hole) {
        supabase.postgrest["holes"].delete {
            filter { eq("id", hole.id) }
        }
    }

    override fun getById(id: Long): Flow<Hole> = flow {
        val hole = supabase.postgrest["holes"].select {
            filter { eq("id", id) }
        }.decodeList<Hole>().firstOrNull()
        if (hole != null) emit(hole)
    }

    override suspend fun getHolesByGameZoneId(gameZoneId: Long): List<Hole> {
        return supabase.postgrest["holes"].select {
            filter { eq("gamezoneid", gameZoneId) }
            order("name", Order.ASCENDING)
        }.decodeList()
    }
}
