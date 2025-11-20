package fr.centuryspine.lsgscores.data.city

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : CityDao {

    override fun getAllCities(): Flow<List<City>> = flow {
        val list = supabase.postgrest["cities"].select {
            order("name", Order.ASCENDING)
        }.decodeList<City>()
        emit(list)
    }

    override suspend fun getAllList(): List<City> {
        return supabase.postgrest["cities"].select {
            order("name", Order.ASCENDING)
        }.decodeList<City>()
    }

    override suspend fun getCityById(cityId: Long): City? {
        return try {
            val list = supabase.postgrest["cities"].select {
                filter { eq("id", cityId) }
            }.decodeList<City>()
            list.firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun insert(city: City): City {
        // Insert and return the inserted city object
        val inserted = supabase.postgrest["cities"].insert(city) { select() }
            .decodeSingle<City>()
        return inserted
    }

    override suspend fun update(city: City) {
        supabase.postgrest["cities"].update(city) {
            filter { eq("id", city.id) }
        }
    }
}
