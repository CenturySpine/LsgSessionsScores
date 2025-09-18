package fr.centuryspine.lsgscores.data.scoring

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoringModeDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : ScoringModeDao {

    override fun getAll(): Flow<List<ScoringMode>> = flow {
        val list = supabase.postgrest["scoring_modes"].select().decodeList<ScoringMode>()
        emit(list)
    }

    override suspend fun getAllList(): List<ScoringMode> {
        return supabase.postgrest["scoring_modes"].select().decodeList()
    }

    override fun getById(id: Int): Flow<ScoringMode?> = flow {
        val one = supabase.postgrest["scoring_modes"].select {
            filter { eq("id", id) }
        }.decodeList<ScoringMode>().firstOrNull()
        emit(one)
    }
}
