package com.example.lsgscores.data.weather

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Simple data class for our internal use
data class WeatherInfo(
    val temperature: Int, // in Celsius
    val description: String,
    val iconCode: String,
    val windSpeedKmh: Int
)

// Open‑Meteo Historical (Archive) API
interface OpenMeteoService {
    @GET("v1/archive")
    suspend fun getArchive(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("hourly") hourly: String = "temperature_2m,weathercode,windspeed_10m",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoArchiveResponse>
}

// Open‑Meteo Forecast API (for current weather)
interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoForecastResponse>
}

data class OpenMeteoArchiveResponse(
    @SerializedName("hourly") val hourly: OpenMeteoHourly?
)

data class OpenMeteoHourly(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("temperature_2m") val temperature2m: List<Double>?,
    @SerializedName("windspeed_10m") val windspeed10m: List<Double>?,
    @SerializedName("weathercode") val weathercode: List<Int>?
)

data class OpenMeteoForecastResponse(
    @SerializedName("current_weather") val currentWeather: OpenMeteoCurrentWeather?
)

data class OpenMeteoCurrentWeather(
    @SerializedName("temperature") val temperature: Double?,
    @SerializedName("windspeed") val windspeed: Double?,
    @SerializedName("weathercode") val weathercode: Int?
)
