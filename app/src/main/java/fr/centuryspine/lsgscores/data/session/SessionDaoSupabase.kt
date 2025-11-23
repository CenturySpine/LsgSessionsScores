package fr.centuryspine.lsgscores.data.session

import fr.centuryspine.lsgscores.data.gamezone.GameZone
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAll(): Flow<List<Session>> =
        supabase.auth.sessionStatus.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> flow {

                    val list =
                        supabase.postgrest["sessions"].select().decodeList<Session>()
                    emit(list)
                }

                else -> flowOf(emptyList())
            }
        }

    override suspend fun getAllList(): List<Session> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["sessions"].select { filter { eq("user_id", uid) } }.decodeList<Session>()
    }

    override fun getById(id: Long): Flow<Session?> = flow {
        val list = supabase.postgrest["sessions"].select { filter { eq("id", id) } }.decodeList<Session>()
        emit(list.firstOrNull())
    }

    override suspend fun insert(session: Session): Long {
        val uid = currentUser.requireUserId()
        val inserted =
            supabase.postgrest["sessions"].insert(session.copy(userId = uid)) { select() }.decodeSingle<Session>()
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
        val list = supabase.postgrest["sessions"].select { filter { eq("isongoing", true); eq("user_id", uid) } }
            .decodeList<Session>()
        return list.firstOrNull()
    }

    override suspend fun getOngoingSessionForCity(cityId: Long): Session? {
        val uid = currentUser.requireUserId()
        // Fetch all game zones for the city
        val zones = supabase.postgrest["game_zones"].select {
            filter { eq("cityid", cityId) }
        }.decodeList<GameZone>()
        val zoneIds = zones.map { it.id }.toSet()
        if (zoneIds.isEmpty()) return null
        // Fetch ongoing sessions for user and filter by gameZoneId in memory
        val list = supabase.postgrest["sessions"].select {
            filter { eq("isongoing", true); eq("user_id", uid) }
        }.decodeList<Session>()
        return list.firstOrNull { it.gameZoneId in zoneIds }
    }

    override suspend fun clearOngoingSessions() {
        val uid = currentUser.requireUserId()
        val body = buildJsonObject { put("isongoing", JsonPrimitive(false)) }
        supabase.postgrest["sessions"].update(body) { filter { eq("isongoing", true); eq("user_id", uid) } }
    }

    override suspend fun clearOngoingSessionsForCity(cityId: Long) {
        val uid = currentUser.requireUserId()
        val body = buildJsonObject { put("isongoing", JsonPrimitive(false)) }
        // Determine sessions to update based on city via game zones
        val zones = supabase.postgrest["game_zones"].select {
            filter { eq("cityid", cityId) }
        }.decodeList<GameZone>()
        val zoneIds = zones.map { it.id }.toSet()
        if (zoneIds.isEmpty()) return
        val sessionsToUpdate = supabase.postgrest["sessions"].select {
            filter { eq("isongoing", true); eq("user_id", uid) }
        }.decodeList<Session>()
            .filter { it.gameZoneId in zoneIds }

        // Update each matching session individually to avoid relying on IN filter support
        for (s in sessionsToUpdate) {
            supabase.postgrest["sessions"].update(body) {
                filter { eq("id", s.id); eq("user_id", uid) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getOngoingSessionFlow(): Flow<Session?> =
        supabase.auth.sessionStatus.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> flow { emit(getOngoingSession()) }
                else -> flowOf(null)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getOngoingSessionFlowForCity(cityId: Long): Flow<Session?> =
        supabase.auth.sessionStatus.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> flow { emit(getOngoingSessionForCity(cityId)) }
                else -> flowOf(null)
            }
        }

    override suspend fun getSessionsByGameZoneId(gameZoneId: Long): List<Session> {
        val uid = currentUser.requireUserId()
        return supabase.postgrest["sessions"].select { filter { eq("gamezoneid", gameZoneId); eq("user_id", uid) } }
            .decodeList<Session>()
    }

    @OptIn(SupabaseExperimental::class)
    override val realtimeSessionFlow: Flow<List<Session>> = supabase.from("sessions").selectAsFlow(Session::id)

//    override fun getRealtimeSessions(): Flow<List<Session>> = realtimeSessionFlow

}
