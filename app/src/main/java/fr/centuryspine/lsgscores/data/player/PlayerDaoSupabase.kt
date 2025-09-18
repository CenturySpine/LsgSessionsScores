package fr.centuryspine.lsgscores.data.player

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : PlayerDao {

    override fun getPlayersByCityId(cityId: Long): Flow<List<Player>> = flow {
        val list = supabase.postgrest["players"].select {
            filter { eq("cityid", cityId) }
            order("name", Order.ASCENDING)
        }.decodeList<Player>()
        emit(list)
    }

    override suspend fun getAll(): List<Player> {
        return supabase.postgrest["players"].select().decodeList<Player>()
    }

    override fun insert(player: Player): Long = runBlocking {
        val inserted = supabase.postgrest["players"].insert(player).decodeSingle<Player>()
        inserted.id
    }

    override fun update(player: Player) {
        runBlocking {
            supabase.postgrest["players"].update(player) {
                filter { eq("id", player.id) }
            }
        }
    }

    override fun delete(player: Player) {
        runBlocking {
            supabase.postgrest["players"].delete {
                filter { eq("id", player.id) }
            }
        }
    }
}
