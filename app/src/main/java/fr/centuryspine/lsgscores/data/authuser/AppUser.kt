package fr.centuryspine.lsgscores.data.authuser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    @SerialName("id")
    val id: String, // UUID from Supabase Auth
    @SerialName("email")
    val email: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("provider")
    val provider: String? = null
)
