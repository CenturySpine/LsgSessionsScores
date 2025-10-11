package fr.centuryspine.lsgscores.data.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) : SessionDao {

    override fun getAll(): Flow<List<Session>> = flow {
        val uid = currentUser.requireUserId()
        val list = supabase.postgrest["sessions"].select { filter { eq("user_id", uid) } }.decodeList<Session>()
        emit(list)
    }

    override suspend fun getAllList(): List<Session> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["sessions"].select { filter { eq("user_id", uid) } }.decodeList<Session>()
    }

    override fun getById(id: Long): Flow<Session?> = flow {
        val uid = currentUser.requireUserId()
        val list = supabase.postgrest["sessions"].select { filter { eq("id", id); eq("user_id", uid) } }.decodeList<Session>()
        emit(list.firstOrNull())
    }

    override suspend fun insert(session: Session): Long {
        val uid = currentUser.requireUserId()
        val inserted = supabase.postgrest["sessions"].insert(session.copy(userId = uid)) { select() }.decodeSingle<Session>()
        return inserted.id
    }

    override suspend fun update(session: Session) {
        // Use a partial update but include key fields we modify from the app (datetime/enddatetime/weatherdata/isongoing/comment)
        val start = fr.centuryspine.lsgscores.data.DateTimeConverters.fromLocalDateTime(session.dateTime)
        val end = fr.centuryspine.lsgscores.data.DateTimeConverters.fromLocalDateTime(session.endDateTime)
        val weatherConv = fr.centuryspine.lsgscores.data.WeatherConverters()
        val weather = weatherConv.fromWeatherInfo(session.weatherData)

        val body = buildJsonObject {
            // Always include isongoing to reflect the current state
            put("isongoing", JsonPrimitive(session.isOngoing))

            // Start datetime (allow null just in case, though it should never be null)
            if (start != null) put("datetime", JsonPrimitive(start)) else put("datetime", JsonNull)

            // End datetime: set to null explicitly when cleared
            if (session.endDateTime != null && end != null) {
                put("enddatetime", JsonPrimitive(end))
            } else {
                put("enddatetime", JsonNull)
            }

            // Weather data if present, otherwise clear
            if (session.weatherData != null && weather != null) {
                put("weatherdata", JsonPrimitive(weather))
            } else {
                put("weatherdata", JsonNull)
            }

            // Optionally include comment if provided (safe no-op if unchanged)
            session.comment?.let { put("comment", JsonPrimitive(it)) }
        }
        run {
            val uid = currentUser.requireUserId()
            supabase.postgrest["sessions"].update(body) { filter { eq("id", session.id); eq("user_id", uid) } }
        }
    }

    override suspend fun delete(session: Session) {
        val uid = currentUser.requireUserId()
        supabase.postgrest["sessions"].delete { filter { eq("id", session.id); eq("user_id", uid) } }
    }

    override suspend fun getOngoingSession(): Session? {
        val uid = currentUser.requireUserId()
        val list = supabase.postgrest["sessions"].select { filter { eq("isongoing", true); eq("user_id", uid) } }.decodeList<Session>()
        return list.firstOrNull()
    }

    override suspend fun getOngoingSessionForCity(cityId: Long): Session? {
        val uid = currentUser.requireUserId()
        val list = supabase.postgrest["sessions"].select { filter { eq("isongoing", true); eq("cityid", cityId); eq("user_id", uid) } }.decodeList<Session>()
        return list.firstOrNull()
    }

    override suspend fun clearOngoingSessions() {
        val uid = currentUser.requireUserId()
        val body = buildJsonObject { put("isongoing", JsonPrimitive(false)) }
        supabase.postgrest["sessions"].update(body) { filter { eq("isongoing", true); eq("user_id", uid) } }
    }

    override suspend fun clearOngoingSessionsForCity(cityId: Long) {
        val uid = currentUser.requireUserId()
        val body = buildJsonObject { put("isongoing", JsonPrimitive(false)) }
        supabase.postgrest["sessions"].update(body) { filter { eq("isongoing", true); eq("cityid", cityId); eq("user_id", uid) } }
    }

    override fun getOngoingSessionFlow(): Flow<Session?> = flow {
        emit(getOngoingSession())
    }

    override fun getOngoingSessionFlowForCity(cityId: Long): Flow<Session?> = flow {
        emit(getOngoingSessionForCity(cityId))
    }

    override suspend fun getSessionsByGameZoneId(gameZoneId: Long): List<Session> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["sessions"].select { filter { eq("gamezoneid", gameZoneId); eq("user_id", uid) } }.decodeList<Session>()
    }
}
