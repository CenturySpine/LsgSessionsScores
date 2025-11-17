package fr.centuryspine.lsgscores.data.authuser

import android.util.Log
import fr.centuryspine.lsgscores.data.player.Player
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
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

        // Defensive extraction of metadata from Map/JSON-like structures used by jan-supabase
        fun anyToStringCompat(value: Any?): String? {
            if (value == null) return null
            return when (value) {
                is String -> value
                is CharSequence -> value.toString()
                is Number, is Boolean -> value.toString()
                is Iterable<*> -> anyToStringCompat(value.firstOrNull())
                else -> {
                    // Fallback: toString(). If it's a JSON primitive string it may be quoted — strip quotes.
                    val s = value.toString()
                    if (s.length >= 2 && s.first() == '"' && s.last() == '"') s.substring(1, s.length - 1) else s
                }
            }?.takeIf { it.isNotBlank() }
        }

        fun extractString(meta: Any?, vararg keys: String): String? = try {
            val m = meta as? Map<*, *> ?: return null
            keys.asSequence()
                .mapNotNull { k -> anyToStringCompat(m[k]) }
                .firstOrNull()
        } catch (_: Throwable) {
            null
        }

        val displayName: String? = extractString(user.userMetadata, "full_name", "name")
        val avatarUrl: String? = extractString(user.userMetadata, "avatar_url", "picture")
        val provider: String? = extractString(user.appMetadata, "provider")

        return try {
            // Check if row exists
            val existing = try {
                supabase.postgrest[tableUsers]
                    .select { filter { eq("id", id) } }
                    .decodeList<AppUser>()
                    .firstOrNull()
            } catch (_: Throwable) {
                null
            }

            val body = AppUser(
                id = id,
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                provider = provider
            )
            if (existing == null) {
                try {
                    supabase.postgrest[tableUsers].insert(body)
                } catch (_: Throwable) {
                }
                val defaultCityId = 1L
                // Create default player and link it to the newly created app_user
                try {
                    val defaultName = displayName ?: email ?: "Player"

                    // 1) Create a new player with cityid = 1 and no photo for this user
                    try {
                        supabase.postgrest["players"].insert(
                            Player(
                                name = defaultName,
                                photoUri = null,
                                cityId = defaultCityId,
                                userId = id
                            )
                        )
                    } catch (t: Throwable) {
                        // Trace explicite si l'INSERT du player échoue (ligne 89 demandée)
                        Log.w(
                            "AppUserDao",
                            "auto-create player: insert failed (user_id=" + id + ", name=" + defaultName + ", cityId=" + defaultCityId + "): " + t.message,
                            t
                        )
                    }

                    // 2) Retrieve the created player's id (follow the same safe pattern used elsewhere)
                    val createdPlayer = try {
                        supabase.postgrest["players"].select {
                            filter { eq("user_id", id); eq("name", defaultName); eq("cityid", defaultCityId) }
                            order("id", Order.DESCENDING)
                            limit(1)
                        }.decodeList<Player>().firstOrNull()
                    } catch (t: Throwable) {
                        Log.w(
                            "AppUserDao",
                            "auto-create player: select failed (user_id=" + id + ", name=" + defaultName + ", cityId=" + defaultCityId + "): " + t.message
                        )
                        null
                    }

                    val playerId = createdPlayer?.id
                    if (playerId != null && playerId > 0) {
                        // 3) Create the link in user_player_link (use typed body to avoid Any-serializer issues)
                        try {
                            supabase.postgrest[tableLink].insert(
                                UserPlayerLink(userId = id, playerId = playerId)
                            )
                        } catch (t: Throwable) {
                            Log.w(
                                "AppUserDao",
                                "auto-link user_player_link: insert failed (user_id=" + id + ", player_id=" + playerId + "): " + t.message,
                                t
                            )
                            // If row exists, update it
                            try {
                                supabase.postgrest[tableLink].update(
                                    UserPlayerLink(userId = id, playerId = playerId)
                                ) { filter { eq("user_id", id) } }
                            } catch (t2: Throwable) {
                                Log.w(
                                    "AppUserDao",
                                    "auto-link user_player_link: update failed (user_id=" + id + ", player_id=" + playerId + "): " + t2.message,
                                    t2
                                )
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.w("AppUserDao", "auto-create player/link failed: ${t.message}")
                }
            } else {
                // Partial update: only send non-null fields to avoid overwriting existing values with nulls
                val updateBody = AppUser(
                    id = id,
                    email = email,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    provider = provider
                )

                try {
                    supabase.postgrest[tableUsers].update(updateBody) { filter { eq("id", id) } }
                } catch (_: Throwable) {
                }

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
        // Wait briefly for auth session restoration. On cold start the session can be null for a short time.
        var attempts = 0
        var userId: String? = null
        while (attempts < 6 && userId == null) {
            userId = try {
                supabase.auth.currentSessionOrNull()?.user?.id
            } catch (_: Throwable) {
                null
            }
            if (userId == null) {
                try {
                    kotlinx.coroutines.delay(250)
                } catch (_: Throwable) {
                }
                attempts++
            }
        }
        val uid = userId ?: return null
        return try {
            val rows = supabase.postgrest[tableLink]
                .select { filter { eq("user_id", uid) } }
                .decodeList<UserPlayerLink>()
            rows.firstOrNull()?.playerId
        } catch (t: Throwable) {
            Log.w("AppUserDao", "getLinkedPlayerId failed: ${t.message}")
            null
        }
    }

}

@kotlinx.serialization.Serializable
data class UserPlayerLink(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("player_id") val playerId: Long
)
