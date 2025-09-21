package fr.centuryspine.lsgscores.data.authuser

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUserDaoSupabase @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val tableUsers = "app_user"
    private val tableLink = "user_player_link"

    suspend fun ensureUserRow(): AppUser? {
        val session = supabase.auth.currentSessionOrNull()
        val user = session?.user ?: return null
        val id = user.id
        val email = user.email
        // Keep it simple and robust; metadata may be absent depending on provider
        val displayName: String? = null
        val avatarUrl: String? = null
        val provider: String? = null

        return try {
            // Check if row exists
            val existing = try {
                supabase.postgrest[tableUsers]
                    .select { filter { eq("id", id) } }
                    .decodeList<AppUser>()
                    .firstOrNull()
            } catch (_: Throwable) { null }

            val body = AppUser(
                id = id,
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                provider = provider
            )
            if (existing == null) {
                try { supabase.postgrest[tableUsers].insert(body) } catch (_: Throwable) {}
            } else {
                try { supabase.postgrest[tableUsers].update(body) { filter { eq("id", id) } } } catch (_: Throwable) {}
            }
            // Return row (best-effort)
            supabase.postgrest[tableUsers]
                .select { filter { eq("id", id) } }
                .decodeList<AppUser>()
                .firstOrNull()
        } catch (t: Throwable) {
            Log.w("AppUserDao", "ensureUserRow failed: ${t.message}")
            null
        }
    }

    suspend fun getLinkedPlayerId(): Long? {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return null
        return try {
            val rows = supabase.postgrest[tableLink]
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserPlayerLink>()
            rows.firstOrNull()?.playerId
        } catch (t: Throwable) {
            Log.w("AppUserDao", "getLinkedPlayerId failed: ${t.message}")
            null
        }
    }

    suspend fun linkToPlayer(playerId: Long): Boolean {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return false
        return try {
            // Check existing link
            val existing = try {
                supabase.postgrest[tableLink]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<UserPlayerLink>()
                    .firstOrNull()
            } catch (_: Throwable) { null }

            val body = UserPlayerLink(userId = userId, playerId = playerId)
            if (existing == null) {
                try { supabase.postgrest[tableLink].insert(body) } catch (_: Throwable) {}
            } else {
                try { supabase.postgrest[tableLink].update(body) { filter { eq("user_id", userId) } } } catch (_: Throwable) {}
            }
            true
        } catch (t: Throwable) {
            Log.w("AppUserDao", "linkToPlayer failed: ${t.message}")
            false
        }
    }
}

@kotlinx.serialization.Serializable
data class UserPlayerLink(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("player_id") val playerId: Long
)
