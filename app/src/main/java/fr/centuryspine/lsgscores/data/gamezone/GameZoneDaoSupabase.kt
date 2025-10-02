package fr.centuryspine.lsgscores.data.gamezone

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameZoneDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : GameZoneDao {

    override fun getGameZonesByCityId(cityId: Long): Flow<List<GameZone>> = flow {
        val list = supabase.postgrest["game_zones"].select {
            filter { eq("cityid", cityId) }
            order("name", Order.ASCENDING)
        }.decodeList<GameZone>()
        emit(list)
    }

    override suspend fun getAll(): List<GameZone> {
        return supabase.postgrest["game_zones"].select().decodeList<GameZone>()
    }

    override suspend fun getGameZoneById(id: Long): GameZone? {
        return try {
            supabase.postgrest["game_zones"].select {
                filter { eq("id", id) }
            }.decodeList<GameZone>().firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun insert(gameZone: GameZone): Long {
        val inserted = supabase.postgrest["game_zones"].insert(gameZone) { select() }.decodeSingle<GameZone>()
        return inserted.id
    }

    override suspend fun update(gameZone: GameZone) {
        supabase.postgrest["game_zones"].update(gameZone) {
            filter { eq("id", gameZone.id) }
        }
    }

    override suspend fun delete(gameZone: GameZone) {
        supabase.postgrest["game_zones"].delete {
            filter { eq("id", gameZone.id) }
        }
    }
}
