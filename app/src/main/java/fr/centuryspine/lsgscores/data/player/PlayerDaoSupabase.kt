package fr.centuryspine.lsgscores.data.player

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : PlayerDao {

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getPlayersByCityId(cityId: Long): Flow<List<Player>> =
        supabase.auth.sessionStatus.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated ->
                    refreshTrigger
                        .onStart { emit(Unit) }
                        .flatMapLatest {
                            flow {
                                try {
                                    val uid = currentUser.requireUserId()
                                    val list = supabase.postgrest["players"].select {
                                        filter { eq("cityid", cityId); eq("user_id", uid) }
                                        order("name", Order.ASCENDING)
                                    }.decodeList<Player>()
                                    emit(list)
                                } catch (_: Throwable) {
                                    emit(emptyList())
                                }
                            }
                        }
                else -> flowOf(emptyList())
            }
        }

    override suspend fun getAll(): List<Player> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["players"].select { filter { eq("user_id", uid) } }.decodeList<Player>()
    }

    override suspend fun getPlayersByCityIdList(cityId: Long): List<Player> {
        return try {
            val uid = currentUser.requireUserId()
            supabase.postgrest["players"].select {
                filter { eq("cityid", cityId); eq("user_id", uid) }
                order("name", Order.ASCENDING)
            }.decodeList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override suspend fun getById(id: Long): Player? {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["players"].select { filter { eq("id", id); eq("user_id", uid) } }.decodeList<Player>().firstOrNull()
    }

    override fun insert(player: Player): Long = runBlocking {
        val uid = currentUser.requireUserId()
        try {
            // Perform insert; ignore body to avoid decode issues when server returns 204 or array
            supabase.postgrest["players"].insert(player.copy(userId = uid))
        } catch (_: Throwable) {
            // Ignore and proceed to fetch by SELECT; network error will surface below if no row found
        }
        // Fetch the most recent matching player in this city by name for current user
        val list = supabase.postgrest["players"].select {
            filter { eq("name", player.name); eq("cityid", player.cityId); eq("user_id", uid) }
            order("id", Order.DESCENDING)
        }.decodeList<Player>()
        val found = list.firstOrNull()
        refreshTrigger.tryEmit(Unit)
        found?.id ?: 0L
    }

    override fun update(player: Player) {
        runBlocking {
            val uid = currentUser.requireUserId()
            supabase.postgrest["players"].update(player.copy(userId = uid)) {
                filter { eq("id", player.id); eq("user_id", uid) }
            }
        }
        refreshTrigger.tryEmit(Unit)
    }

    override fun delete(player: Player) {
        runBlocking {
            val uid = currentUser.requireUserId()
            supabase.postgrest["players"].delete {
                filter { eq("id", player.id); eq("user_id", uid) }
            }
        }
        refreshTrigger.tryEmit(Unit)
    }
}
