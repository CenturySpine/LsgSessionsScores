package fr.centuryspine.lsgscores.utils

import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.centuryspine.lsgscores.data.DateTimeConverters
import fr.centuryspine.lsgscores.data.WeatherConverters
import fr.centuryspine.lsgscores.data.city.CityDao
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.player.PlayerDao
import fr.centuryspine.lsgscores.data.scoring.ScoringModeDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleDao
import fr.centuryspine.lsgscores.data.session.PlayedHoleScoreDao
import fr.centuryspine.lsgscores.data.session.SessionDao
import fr.centuryspine.lsgscores.data.session.TeamDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import fr.centuryspine.lsgscores.BuildConfig

@Singleton
class MigrationManager @Inject constructor(
    private val cityDao: CityDao,
    private val gameZoneDao: GameZoneDao,
    private val scoringModeDao: ScoringModeDao,
    private val playerDao: PlayerDao,
    private val holeDao: HoleDao,
    private val sessionDao: SessionDao,
    private val teamDao: TeamDao,
    private val playedHoleDao: PlayedHoleDao,
    private val playedHoleScoreDao: PlayedHoleScoreDao,
    private val supabase: SupabaseClient,
    private val storageHelper: SupabaseStorageHelper,
    @ApplicationContext private val context: Context
) {

    // Report of what has been inserted remotely. Note: Local Room DB is NEVER mutated by this manager.
    data class MigrationReport(
        val cities: Int = 0,
        val gameZones: Int = 0,
        val scoringModes: Int = 0,
        val players: Int = 0,
        val holes: Int = 0,
        val sessions: Int = 0,
        val teams: Int = 0,
        val playedHoles: Int = 0,
        val playedHoleScores: Int = 0,
    )

    private val TAG = "MigrationManager"

    suspend fun migrateAll(onStep: (String) -> Unit = {}): MigrationReport = withContext(Dispatchers.IO) {
        try {
            // Safety: migration is debug-only, never run in release builds
            if (!BuildConfig.DEBUG) {
                throw IllegalStateException("Migration is debug-only and cannot run in release builds")
            }
            Log.i(TAG, "Starting migration (data + images)...")
            val postgrest = supabase.postgrest
            val weatherConv = WeatherConverters()

            onStep("Nettoyage du storage (buckets)…")
            // Toujours vider les buckets pour réimporter les images comme la première fois
            try {
                Log.i(TAG, "Clearing storage buckets via RPC reset_storage_buckets()…")
                Log.d(TAG, "[DLOG] call RPC reset_storage_buckets()")
                supabase.postgrest.rpc("reset_storage_buckets")
                Log.i(TAG, "Storage buckets cleared")
            } catch (t: Throwable) {
                Log.w(TAG, "reset_storage_buckets RPC not available or failed: ${t.message}")
            }
            try {
                onStep("Suppression des données (tables)…")
                Log.i(TAG, "Resetting remote data via RPC reset_all_data()…")
                Log.d(TAG, "[DLOG] call RPC reset_all_data()")
                supabase.postgrest.rpc("reset_all_data")
                Log.i(TAG, "Remote data reset done")
            } catch (t: Throwable) {
                Log.w(TAG, "reset_all_data RPC not available or failed: ${t.message}")
            }

            onStep("Migration des villes…")
            // 1) Cities
            val cities = cityDao.getAllList()
            for (c in cities) {
                safeUpsert("cities", mapOf("id" to c.id, "name" to c.name))
            }

            // 2) Game zones
            onStep("Migration des zones de jeu…")
            val gameZones = gameZoneDao.getAll()
            for (gz in gameZones) {
                safeUpsert(
                    "game_zones",
                    mapOf(
                        "id" to gz.id,
                        "name" to gz.name,
                        "cityId" to gz.cityId
                    )
                )
            }

            // 3) Scoring modes
            onStep("Migration des modes de score…")
            val scoringModes = scoringModeDao.getAllList()
            for (sm in scoringModes) {
                safeUpsert(
                    "scoring_modes",
                    mapOf(
                        "id" to sm.id,
                        "name" to sm.name,
                        "description" to sm.description
                    )
                )
            }

            // 4) Players (with photo upload)
            onStep("Migration des joueurs…")
            val players = playerDao.getAll()
            var playersCount = 0
            for (p in players) {
                val newPhoto = p.photoUri?.let { ensureRemoteUrlForPlayer(p.id, it) }
                safeUpsert(
                    "players",
                    mapOf(
                        "id" to p.id,
                        "name" to p.name,
                        "photoUri" to (newPhoto ?: p.photoUri),
                        "cityId" to p.cityId
                    )
                )
                playersCount++
            }

            // 5) Holes (with photos)
            onStep("Migration des trous…")
            val holes = holeDao.getAll()
            var holesCount = 0
            for (h in holes) {
                val newStart = h.startPhotoUri?.let { ensureRemoteUrlForHole(h.id, SupabaseStorageHelper.PhotoType.START, it) }
                val newEnd = h.endPhotoUri?.let { ensureRemoteUrlForHole(h.id, SupabaseStorageHelper.PhotoType.END, it) }
                safeUpsert(
                    "holes",
                    mapOf(
                        "id" to h.id,
                        "name" to h.name,
                        "gameZoneId" to h.gameZoneId,
                        "description" to h.description,
                        "distance" to h.distance,
                        "par" to h.par,
                        "startPhotoUri" to (newStart ?: h.startPhotoUri),
                        "endPhotoUri" to (newEnd ?: h.endPhotoUri)
                    )
                )
                holesCount++
            }

            // 6) Sessions
            onStep("Migration des sessions…")
            val sessions = sessionDao.getAllList()
            var sessionsCount = 0
            for (s in sessions) {
                val dateText = DateTimeConverters.fromLocalDateTime(s.dateTime)
                val endDateText = DateTimeConverters.fromLocalDateTime(s.endDateTime)
                val weatherJson = weatherConv.fromWeatherInfo(s.weatherData)
                safeUpsert(
                    "sessions",
                    mapOf(
                        "id" to s.id,
                        "dateTime" to dateText,
                        "endDateTime" to endDateText,
                        "sessionType" to s.sessionType.name,
                        "scoringModeId" to s.scoringModeId,
                        "gameZoneId" to s.gameZoneId,
                        "comment" to s.comment,
                        "isOngoing" to s.isOngoing,
                        "weatherData" to weatherJson,
                        "cityId" to s.cityId
                    )
                )
                sessionsCount++
            }

            // 7) Teams
            onStep("Migration des équipes…")
            val teams = teamDao.getAll()
            var teamsCount = 0
            for (t in teams) {
                safeUpsert(
                    "teams",
                    mapOf(
                        "id" to t.id,
                        "sessionId" to t.sessionId,
                        "player1Id" to t.player1Id,
                        "player2Id" to t.player2Id
                    )
                )
                teamsCount++
            }

            // 8) Played holes
            onStep("Migration des trous joués…")
            val playedHoles = playedHoleDao.getAll()
            var playedHolesCount = 0
            for (ph in playedHoles) {
                safeUpsert(
                    "played_holes",
                    mapOf(
                        "id" to ph.id,
                        "sessionId" to ph.sessionId,
                        "holeId" to ph.holeId,
                        "gameModeId" to ph.gameModeId,
                        "position" to ph.position
                    )
                )
                playedHolesCount++
            }

            // 9) Played hole scores
            onStep("Migration des scores…")
            val scores = playedHoleScoreDao.getAll()
            var scoresCount = 0
            for (sc in scores) {
                safeUpsert(
                    "played_hole_scores",
                    mapOf(
                        "id" to sc.id,
                        "playedHoleId" to sc.playedHoleId,
                        "teamId" to sc.teamId,
                        "strokes" to sc.strokes
                    )
                )
                scoresCount++
            }

            val report = MigrationReport(
                cities = cities.size,
                gameZones = gameZones.size,
                scoringModes = scoringModes.size,
                players = playersCount,
                holes = holesCount,
                sessions = sessionsCount,
                teams = teamsCount,
                playedHoles = playedHolesCount,
                playedHoleScores = scoresCount
            )

            // Renumber all IDs from 1..n and then align sequences
            onStep("Renumérotation des IDs…")
            try {
                Log.i(TAG, "Renumbering IDs via RPC renumber_all_ids()…")
                supabase.postgrest.rpc("renumber_all_ids")
                Log.i(TAG, "IDs renumbered")
                onStep("Renumérotation des IDs terminée")
            } catch (t: Throwable) {
                Log.w(TAG, "renumber_all_ids RPC not available or failed: ${t.message}")
            }

            onStep("Réalignement des séquences d'identité…")
            try {
                Log.i(TAG, "Aligning identity sequences via RPC align_all_sequences()…")
                supabase.postgrest.rpc("align_all_sequences")
                Log.i(TAG, "Sequences aligned")
                onStep("Réalignement des séquences terminé")
            } catch (t: Throwable) {
                Log.w(TAG, "align_all_sequences RPC not available or failed: ${t.message}")
            }

            Log.i(TAG, "Migration completed: $report")
            report
        } catch (ce: CancellationException) {
            Log.w(TAG, "Migration cancelled")
            throw ce
        } catch (t: Throwable) {
            Log.e(TAG, "Migration failed", t)
            throw t
        }
    }

    private suspend fun ensureRemoteUrlForPlayer(playerId: Long, uriStr: String): String? {
        val uri = Uri.parse(uriStr)
        return try {
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                // Already a remote URL: keep as-is (don’t reupload)
                Log.d(TAG, "[DLOG] player=$playerId photo already remote -> keep $uriStr")
                uriStr
            } else {
                // Local content/file path: upload to Supabase bucket and return the public URL
                val remote = storageHelper.uploadPlayerPhoto(playerId, uri)
                Log.d(TAG, "[DLOG] player=$playerId photo local -> uploaded: $uriStr -> $remote")
                remote
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to handle player image from $uriStr: ${e.message}")
            null
        }
    }

    private suspend fun ensureRemoteUrlForHole(holeId: Long, type: SupabaseStorageHelper.PhotoType, uriStr: String): String? {
        val uri = Uri.parse(uriStr)
        return try {
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                // Already a remote URL: keep as-is
                uriStr
            } else {
                storageHelper.uploadHolePhoto(holeId, type, uri)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to handle hole image from $uriStr: ${e.message}")
            null
        }
    }

    private suspend fun safeUpsert(table: String, data: Map<String, Any?>) {
        // Convert the arbitrary Map<String, Any?> to a JsonObject so Kotlinx Serialization
        // doesn't try to find a serializer for `Any` (which causes the runtime error).
        val json = mapToJsonObject(data)
        try {
            Log.d(TAG, "[DLOG] upsert table=$table id=${data["id"]} keys=${data.keys}")
            // Use upsert semantics where supported; fall back to insert if necessary
            supabase.postgrest[table].upsert(json, onConflict = "id")
        } catch (e: Throwable) {
            Log.w(TAG, "Upsert failed for $table on conflict=id, falling back to insert: ${e.message}")
            try {
                Log.d(TAG, "[DLOG] insert table=$table id=${data["id"]}")
                supabase.postgrest[table].insert(json)
            } catch (inner: Throwable) {
                Log.e(TAG, "Insert failed for $table: ${inner.message}")
                throw inner
            }
        }
    }

    // --- Helpers to convert to kotlinx.serialization JSON ---
    private fun mapToJsonObject(map: Map<String, Any?>): kotlinx.serialization.json.JsonObject {
        return kotlinx.serialization.json.buildJsonObject {
            map.forEach { (key, value) ->
                // Postgres folds unquoted identifiers to lowercase, and Supabase's
                // PostgREST schema cache uses those lowercase names. Our Room entities
                // use camelCase (e.g., cityId), so we must lowercase keys to match
                // DB column names (e.g., cityid) to avoid "column not found" errors.
                val dbKey = key.lowercase()
                put(dbKey, anyToJsonElement(value))
            }
        }
    }

    private fun anyToJsonElement(value: Any?): kotlinx.serialization.json.JsonElement {
        return when (value) {
            null -> kotlinx.serialization.json.JsonNull
            is kotlinx.serialization.json.JsonElement -> value
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            is Int -> kotlinx.serialization.json.JsonPrimitive(value)
            is Long -> kotlinx.serialization.json.JsonPrimitive(value)
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is Float -> kotlinx.serialization.json.JsonPrimitive(value)
            is Double -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value.toDouble())
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
    }
}
