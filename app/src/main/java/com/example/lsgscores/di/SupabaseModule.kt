package com.example.lsgscores.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton
import com.example.lsgscores.BuildConfig
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
            install(Storage)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest {
        return client.pluginManager.getPlugin(Postgrest)
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth {
        return client.pluginManager.getPlugin(Auth)
    }

    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage {
        return client.pluginManager.getPlugin(Storage)
    }

    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime {
        return client.pluginManager.getPlugin(Realtime)
    }
}