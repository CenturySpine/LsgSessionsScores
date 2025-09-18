package fr.centuryspine.lsgscores.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import fr.centuryspine.lsgscores.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.android.Android

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val url = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        require(url.isNotBlank()) { "SUPABASE_URL is not set. Define supabase.url in local.properties." }
        require(anonKey.isNotBlank()) { "SUPABASE_ANON_KEY is not set. Define supabase.anonKey in local.properties." }

        return createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
            httpEngine = Android.create()
            install(Postgrest)
            install(Storage)
        }
    }
}
