package com.example.lsgscores.di

import android.content.Context
import com.example.lsgscores.data.weather.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.centuryspine.lsgscores.R
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    @Provides
    @Singleton
    fun provideWeatherRepository(@ApplicationContext context: Context): WeatherRepository {
        val apiKey = context.getString(R.string.openweather_api_key)
        return WeatherRepository(apiKey)
    }
}