package fr.centuryspine.lsgscores.data.player

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : PlayerDao {

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getPlayersByCityId(cityId: Long): Flow<List<Player>> =
        refreshTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    try {
                        val list = supabase.postgrest["players"].select {
                            filter { eq("cityid", cityId) }
                            order("name", Order.ASCENDING)
                        }.decodeList<Player>()
                        emit(list)
                    } catch (_: Throwable) {
                        emit(emptyList())
                    }
                }
            }

    override suspend fun getAll(): List<Player> {
        return supabase.postgrest["players"].select().decodeList<Player>()
    }

    override suspend fun getPlayersByCityIdList(cityId: Long): List<Player> {
        return try {
            supabase.postgrest["players"].select {
                filter { eq("cityid", cityId) }
                order("name", Order.ASCENDING)
            }.decodeList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override suspend fun getById(id: Long): Player? {
        return supabase.postgrest["players"].select { filter { eq("id", id) } }.decodeList<Player>().firstOrNull()
    }

    override fun insert(player: Player): Long = runBlocking {
        try {
            // Perform insert; ignore body to avoid decode issues when server returns 204 or array
            supabase.postgrest["players"].insert(player)
        } catch (_: Throwable) {
            // Ignore and proceed to fetch by SELECT; network error will surface below if no row found
        }
        // Fetch the most recent matching player in this city by name
        val list = supabase.postgrest["players"].select {
            filter { eq("name", player.name); eq("cityid", player.cityId) }
            order("id", Order.DESCENDING)
        }.decodeList<Player>()
        val found = list.firstOrNull()
        refreshTrigger.tryEmit(Unit)
        found?.id ?: 0L
    }

    override fun update(player: Player) {
        runBlocking {
            supabase.postgrest["players"].update(player) {
                filter { eq("id", player.id) }
            }
        }
        refreshTrigger.tryEmit(Unit)
    }

    override fun delete(player: Player) {
        runBlocking {
            supabase.postgrest["players"].delete {
                filter { eq("id", player.id) }
            }
        }
        refreshTrigger.tryEmit(Unit)
    }
}
