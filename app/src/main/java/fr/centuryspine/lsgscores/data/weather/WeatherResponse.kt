package com.example.lsgscores.data.weather

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("weather")
    val weather: List<Weather>,
    @SerializedName("main")
    val main: Main,
    @SerializedName("wind")
    val wind: Wind,
    @SerializedName("name")
    val name: String
)

data class Weather(
    @SerializedName("id")
    val id: Int,
    @SerializedName("main")
    val main: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("icon")
    val icon: String
)

data class Main(
    @SerializedName("temp")
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("humidity")
    val humidity: Int
)

data class Wind(
    @SerializedName("speed")
    val speed: Double, // in m/s, will convert to km/h
    @SerializedName("deg")
    val deg: Int
)

// Simple data class for our internal use
data class WeatherInfo(
    val temperature: Int, // in Celsius
    val description: String,
    val iconCode: String,
    val windSpeedKmh: Int
)



interface WeatherService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<WeatherResponse>

    // Historical weather (Time Machine). Note: availability depends on API plan and time range limitations.
    @GET("onecall/timemachine")
    suspend fun getHistoricalWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("dt") timestamp: Long,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<OneCallTimemachineResponse>
}

// Minimal subset of One Call Time Machine response used by the app
data class OneCallTimemachineResponse(
    @SerializedName("current") val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temp") val temp: Double?,
    @SerializedName("wind_speed") val windSpeed: Double?,
    @SerializedName("weather") val weather: List<Weather>?
)

// Service for History API host (history.openweathermap.org)
interface HistoryWeatherService {
    @retrofit2.http.GET("history/city")
    suspend fun getCityHistory(
        @retrofit2.http.Query("lat") latitude: Double,
        @retrofit2.http.Query("lon") longitude: Double,
        @retrofit2.http.Query("type") type: String = "hour",
        @retrofit2.http.Query("start") start: Long,
        @retrofit2.http.Query("end") end: Long,
        @retrofit2.http.Query("appid") apiKey: String,
        @retrofit2.http.Query("units") units: String = "metric"
    ): retrofit2.Response<HistoryCityResponse>
}

// Minimal subset for History City API
data class HistoryCityResponse(
    @SerializedName("list") val list: List<HistoryItem>?
)

data class HistoryItem(
    @SerializedName("dt") val dt: Long?,
    @SerializedName("main") val main: Main?,
    @SerializedName("wind") val wind: Wind?,
    @SerializedName("weather") val weather: List<Weather>?
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
    ): retrofit2.Response<OpenMeteoArchiveResponse>
}

// Open‑Meteo Forecast API (for current weather)
interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("timezone") timezone: String = "auto"
    ): retrofit2.Response<OpenMeteoForecastResponse>
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
