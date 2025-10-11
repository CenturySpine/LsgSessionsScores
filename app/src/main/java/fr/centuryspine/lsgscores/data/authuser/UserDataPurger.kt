package fr.centuryspine.lsgscores.data.authuser

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Purges all application data that belongs to the currently authenticated user.
 * This does not delete the Supabase Auth account (requires admin privileges),
 * but removes all rows in app tables filtered by user_id and finally the app_user row.
 */
@Singleton
class UserDataPurger @Inject constructor(
    private val supabase: SupabaseClient,
    private val currentUser: CurrentUserProvider,
    private val appUserDao: AppUserDaoSupabase
) {
    private val TAG = "UserDataPurger"

    /**
     * Delete all rows for the current user in a dependency-safe order.
     * Adds strong guardrails:
     * - Validates that the current user id is a proper UUID
     * - For each table, logs pre-delete count and verifies post-delete count is zero for that user
     * If any step fails, the process stops and the original error is thrown.
     */
    suspend fun purgeAllForCurrentUser() {
        val uid = currentUser.requireUserId()
        assertValidUserId(uid)
        try {
            // 1) Session-related data (scores -> played holes -> teams -> sessions)
            safeDeleteStrict("played_hole_scores", uid)
            safeDeleteStrict("played_holes", uid)
            safeDeleteStrict("teams", uid)
            safeDeleteStrict("sessions", uid)

            // 2) Player data
            safeDeleteStrict("players", uid)

            // 3) Course data (holes first, then zones)
            safeDeleteStrict("holes", uid)
            safeDeleteStrict("game_zones", uid)

            // 4) Strict deletion now that RLS allows DELETE
            safeDeleteUserLinkStrict(uid)
            safeDeleteAppUserStrict(uid)
        } catch (t: Throwable) {
            Log.e(TAG, "purgeAllForCurrentUser failed: ${t.message}", t)
            throw t
        }
    }

    private fun assertValidUserId(userId: String) {
        if (userId.isBlank()) throw IllegalStateException("Invalid user id: blank")
        try {
            UUID.fromString(userId)
        } catch (_: Throwable) {
            throw IllegalStateException("Invalid user id format (expected UUID)")
        }
    }

    private suspend fun countForUser(table: String, userId: String): Int {
        val rows = try {
            supabase.postgrest[table].select {
                filter { eq("user_id", userId) }
            }.decodeList<JsonObject>()
        } catch (_: Throwable) {
            emptyList()
        }
        return rows.size
    }

    private suspend fun safeDeleteStrict(table: String, userId: String) {
        val before = countForUser(table, userId)
        Log.d(TAG, "[$table] rows for user before delete: $before")
        try {
            supabase.postgrest[table].delete {
                filter { eq("user_id", userId) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Delete from $table failed: ${t.message}")
            throw t
        }
        val after = countForUser(table, userId)
        Log.d(TAG, "[$table] rows for user after delete: $after")
        if (before > 0 && after > 0) {
            throw IllegalStateException("Failed to delete some rows from $table for user $userId (remaining=$after)")
        }
    }

    private suspend fun safeDeleteUserLinkStrict(userId: String) {
        val table = "user_player_link"
        val before = countForUser(table, userId)
        Log.d(TAG, "[$table] rows for user before delete: $before")
        try {
            supabase.postgrest[table].delete { filter { eq("user_id", userId) } }
        } catch (t: Throwable) {
            Log.w(TAG, "Delete $table failed: ${t.message}")
            throw t
        }
        val after = countForUser(table, userId)
        Log.d(TAG, "[$table] rows for user after delete: $after")
        if (before > 0 && after > 0) {
            throw IllegalStateException("Failed to delete some rows from $table for user $userId (remaining=$after)")
        }
    }

    private suspend fun safeDeleteAppUserStrict(userId: String) {
        // app_user uses id (PK) instead of user_id
        val before = try {
            supabase.postgrest["app_user"].select { filter { eq("id", userId) } }.decodeList<JsonObject>().size
        } catch (_: Throwable) { 0 }
        Log.d(TAG, "[app_user] rows for user before delete: $before")
        try {
            supabase.postgrest["app_user"].delete { filter { eq("id", userId) } }
        } catch (t: Throwable) {
            Log.w(TAG, "Delete app_user failed: ${t.message}")
            throw t
        }
        val after = try {
            supabase.postgrest["app_user"].select { filter { eq("id", userId) } }.decodeList<JsonObject>().size
        } catch (_: Throwable) { 0 }
        Log.d(TAG, "[app_user] rows for user after delete: $after")
        if (before > 0 && after > 0) {
            throw IllegalStateException("Failed to delete app_user row for user $userId")
        }
    }

}