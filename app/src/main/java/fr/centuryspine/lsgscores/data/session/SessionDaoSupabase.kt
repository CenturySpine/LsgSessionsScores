package fr.centuryspine.lsgscores.data.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) : SessionDao {

    override fun getAll(): Flow<List<Session>> = flow {
        val list = supabase.postgrest["sessions"].select().decodeList<Session>()
        emit(list)
    }

    override suspend fun getAllList(): List<Session> {
        return supabase.postgrest["sessions"].select().decodeList<Session>()
    }

    override fun getById(id: Int): Flow<Session?> = flow {
        val list = supabase.postgrest["sessions"].select { filter { eq("id", id) } }.decodeList<Session>()
        emit(list.firstOrNull())
    }

    override suspend fun insert(session: Session): Long {
        val inserted = supabase.postgrest["sessions"].insert(session) { select() }.decodeSingle<Session>()
        return inserted.id
    }

    override suspend fun update(session: Session) {
        // Use a partial update to ensure fields with default values (like isOngoing=false)
        // are explicitly written, as kotlinx.serialization may omit default values otherwise.
        val body = buildJsonObject {
            // Always include isongoing to reflect the current state
            put("isongoing", JsonPrimitive(session.isOngoing))
            // Include enddatetime if present
            val end = fr.centuryspine.lsgscores.data.DateTimeConverters.fromLocalDateTime(session.endDateTime)
            if (end != null) put("enddatetime", JsonPrimitive(end))
            // Optionally include comment if provided (safe no-op if unchanged)
            session.comment?.let { put("comment", JsonPrimitive(it)) }
        }
        supabase.postgrest["sessions"].update(body) { filter { eq("id", session.id) } }
    }

    override suspend fun delete(session: Session) {
        supabase.postgrest["sessions"].delete { filter { eq("id", session.id) } }
    }

    override suspend fun getOngoingSession(): Session? {
        val list = supabase.postgrest["sessions"].select { filter { eq("isongoing", true) } }.decodeList<Session>()
        return list.firstOrNull()
    }

    override suspend fun getOngoingSessionForCity(cityId: Long): Session? {
        val list = supabase.postgrest["sessions"].select { filter { eq("isongoing", true); eq("cityid", cityId) } }.decodeList<Session>()
        return list.firstOrNull()
    }

    override suspend fun clearOngoingSessions() {
        val body = buildJsonObject { put("isongoing", JsonPrimitive(false)) }
        supabase.postgrest["sessions"].update(body) { filter { eq("isongoing", true) } }
    }

    override suspend fun clearOngoingSessionsForCity(cityId: Long) {
        val body = buildJsonObject { put("isongoing", JsonPrimitive(false)) } 
        supabase.postgrest["sessions"].update(body) { filter { eq("isongoing", true); eq("cityid", cityId) } }
    }

    override fun getOngoingSessionFlow(): Flow<Session?> = flow {
        emit(getOngoingSession())
    }

    override fun getOngoingSessionFlowForCity(cityId: Long): Flow<Session?> = flow {
        emit(getOngoingSessionForCity(cityId))
    }

    override suspend fun getSessionsByGameZoneId(gameZoneId: Long): List<Session> {
        return supabase.postgrest["sessions"].select { filter { eq("gamezoneid", gameZoneId) } }.decodeList<Session>()
    }
}
