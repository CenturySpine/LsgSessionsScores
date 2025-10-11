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
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : GameZoneDao {

    override fun getGameZonesByCityId(cityId: Long): Flow<List<GameZone>> = flow {
        val uid = currentUser.requireUserId()
        val list = supabase.postgrest["game_zones"].select {
            filter { eq("cityid", cityId); eq("user_id", uid) }
            order("name", Order.ASCENDING)
        }.decodeList<GameZone>()
        emit(list)
    }

    override suspend fun getAll(): List<GameZone> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["game_zones"].select { filter { eq("user_id", uid) } }.decodeList<GameZone>()
    }

    override suspend fun getGameZoneById(id: Long): GameZone? {
        return try {
            val uid = currentUser.requireUserId()
            supabase.postgrest["game_zones"].select {
                filter { eq("id", id); eq("user_id", uid) }
            }.decodeList<GameZone>().firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun insert(gameZone: GameZone): Long {
        val uid = currentUser.requireUserId()
        val inserted = supabase.postgrest["game_zones"].insert(gameZone.copy(userId = uid)) { select() }.decodeSingle<GameZone>()
        return inserted.id
    }

    override suspend fun update(gameZone: GameZone) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["game_zones"].update(gameZone.copy(userId = uid)) {
            filter { eq("id", gameZone.id); eq("user_id", uid) }
        }
    }

    override suspend fun delete(gameZone: GameZone) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["game_zones"].delete {
            filter { eq("id", gameZone.id); eq("user_id", uid) }
        }
    }
}
