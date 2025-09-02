package com.example.lsgscores.data.weather

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class WeatherRepository(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private const val TAG = "WeatherRepository"
    }

    private val weatherService: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }

    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherInfo? {
        return try {
            val response = weatherService.getCurrentWeather(latitude, longitude, apiKey)

            if (response.isSuccessful && response.body() != null) {
                val weatherResponse = response.body()!!

                // Convert to our simplified structure
                WeatherInfo(
                    temperature = weatherResponse.main.temp.roundToInt(),
                    description = weatherResponse.weather.firstOrNull()?.description ?: "Unknown",
                    iconCode = weatherResponse.weather.firstOrNull()?.icon ?: "01d",
                    windSpeedKmh = (weatherResponse.wind.speed * 3.6).roundToInt() // m/s to km/h
                )
            } else {
                Log.e(TAG, "Weather API error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather data", e)
            null
        }
    }
}