package fr.centuryspine.lsgscores.data.gamezone

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameZoneDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : GameZoneDao {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getGameZonesByCityId(cityId: Long): Flow<List<GameZone>> =
        supabase.auth.sessionStatus.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> flow {
                    currentUser.requireUserId()
                    val list = supabase.postgrest["game_zones"].select {
                        filter { eq("cityid", cityId); }
                        order("name", Order.ASCENDING)
                    }.decodeList<GameZone>()
                    emit(list)
                }

                else -> flowOf(emptyList())
            }
        }

    override suspend fun getAll(): List<GameZone> {
        currentUser.requireUserId()
        return supabase.postgrest["game_zones"].select { }.decodeList<GameZone>()
    }

    override suspend fun getGameZoneById(id: Long): GameZone? {
        return try {
            currentUser.requireUserId()
            supabase.postgrest["game_zones"].select {
                filter { eq("id", id) }
            }.decodeList<GameZone>().firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun insert(gameZone: GameZone): Long {
        val uid = currentUser.requireUserId()
        val inserted =
            supabase.postgrest["game_zones"].insert(gameZone.copy(userId = uid)) { select() }.decodeSingle<GameZone>()
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
