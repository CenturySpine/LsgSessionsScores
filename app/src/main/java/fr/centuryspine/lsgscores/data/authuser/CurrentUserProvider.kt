package fr.centuryspine.lsgscores.data.authuser

import javax.inject.Inject
import javax.inject.Singleton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth

interface CurrentUserProvider {
    fun userIdOrNull(): String?
    fun requireUserId(): String
}

@Singleton
class CurrentUserProviderImpl @Inject constructor(
    private val supabase: SupabaseClient
) : CurrentUserProvider {
    override fun userIdOrNull(): String? = supabase.auth.currentSessionOrNull()?.user?.id
    override fun requireUserId(): String = userIdOrNull()
        ?: throw IllegalStateException("User not authenticated")
}
