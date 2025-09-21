package fr.centuryspine.lsgscores.data.hole

import fr.centuryspine.lsgscores.data.gamezone.GameZone
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoleDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : HoleDao {

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val addedHoleFlow = MutableSharedFlow<Hole>(extraBufferCapacity = 16)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getHolesByCityId(cityId: Long): Flow<List<Hole>> =
        refreshTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    // Fetch game zones for this city, then fetch holes for each zone and merge
                    val zones = supabase.postgrest["game_zones"].select {
                        filter { eq("cityid", cityId) }
                        order("name", Order.ASCENDING)
                    }.decodeList<GameZone>()
                    val zoneIds = zones.map { it.id }.toSet()
                    val result = mutableListOf<Hole>()
                    for (gz in zones) {
                        val holes = supabase.postgrest["holes"].select {
                            filter { eq("gamezoneid", gz.id) }
                            order("name", Order.ASCENDING)
                        }.decodeList<Hole>()
                        result += holes
                    }
                    var current = result.toList()
                    emit(current)
                    // Incrementally append newly added holes without reloading everything
                    addedHoleFlow.collect { newHole ->
                        if (zoneIds.contains(newHole.gameZoneId)) {
                            if (current.none { it.id == newHole.id }) {
                                current = current + newHole
                                emit(current)
                            }
                        }
                    }
                }
            }

    override suspend fun getAll(): List<Hole> {
        return supabase.postgrest["holes"].select().decodeList<Hole>()
    }

    override suspend fun insert(hole: Hole): Long {
        // Insert without assuming the server returns the inserted row in the body.
        // Some PostgREST setups use return=minimal which yields an empty body, causing decode errors.
        try {
            supabase.postgrest["holes"].insert(hole)
        } catch (_: Throwable) {
            // Ignore insert body/decoding issues and proceed to fetch by SELECT
        }
        // Fetch the most recent matching hole for the same game zone and name
        val list = supabase.postgrest["holes"].select {
            filter { eq("name", hole.name); eq("gamezoneid", hole.gameZoneId) }
            order("id", Order.DESCENDING)
        }.decodeList<Hole>()
        val found = list.firstOrNull()
        if (found != null) {
            // Emit incremental add to current list consumers (no full reload)
            addedHoleFlow.tryEmit(found)
        } else {
            // Fallback: trigger full reload if we couldn't fetch the row
            refreshTrigger.tryEmit(Unit)
        }
        return found?.id ?: 0L
    }

    override suspend fun update(hole: Hole) {
        supabase.postgrest["holes"].update(hole) {
            filter { eq("id", hole.id) }
        }
        refreshTrigger.tryEmit(Unit)
    }

    override suspend fun delete(hole: Hole) {
        supabase.postgrest["holes"].delete {
            filter { eq("id", hole.id) }
        }
        refreshTrigger.tryEmit(Unit)
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
