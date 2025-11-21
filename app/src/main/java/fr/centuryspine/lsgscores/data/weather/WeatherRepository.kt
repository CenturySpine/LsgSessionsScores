package com.example.lsgscores.data.weather

import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class WeatherRepository(private val apiKey: String) {

    companion object {
        private const val BASE_URL_OPEN_METEO = "https://archive-api.open-meteo.com/"
        private const val BASE_URL_OPEN_METEO_FORECAST = "https://api.open-meteo.com/"
        private const val TAG = "WeatherRepository"
    }

    private val serviceOpenMeteo: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_OPEN_METEO)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoService::class.java)
    }

    private val serviceOpenMeteoForecast: OpenMeteoForecastService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_OPEN_METEO_FORECAST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoForecastService::class.java)
    }

    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherInfo? {
        return try {
            val response = serviceOpenMeteoForecast.getForecast(latitude, longitude)
            if (response.isSuccessful) {
                val cw = response.body()?.currentWeather
                if (cw != null) {
                    val code = cw.weathercode ?: 0
                    WeatherInfo(
                        temperature = (cw.temperature ?: 0.0).roundToInt(),
                        description = mapOpenMeteoCodeToDescription(code),
                        iconCode = mapOpenMeteoCodeToIcon(code),
                        windSpeedKmh = (cw.windspeed ?: 0.0).roundToInt()
                    )
                } else {
                    Log.w(TAG, "Open‑Meteo current weather: empty body")
                    null
                }
            } else {
                Log.e(
                    TAG,
                    "Open‑Meteo current weather error: ${response.code()} - ${response.message()} - ${
                        response.errorBody()?.safeString()
                    }"
                )
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch current weather (Open‑Meteo)", e)
            null
        }
    }

    suspend fun getHistoricalWeather(latitude: Double, longitude: Double, unixTimestamp: Long): WeatherInfo? {
        return try {
            // Use Open‑Meteo Archive API only (no OpenWeatherMap usage)
            var result: WeatherInfo? = null
            try {
                val zdt = java.time.ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(unixTimestamp),
                    java.time.ZoneId.systemDefault()
                )
                val dateStr = zdt.toLocalDate().toString() // yyyy-MM-dd
                val targetHourStr = zdt.toLocalDate().toString() + "T" + String.format("%02d:00", zdt.hour)

                val omResp = serviceOpenMeteo.getArchive(
                    latitude = latitude,
                    longitude = longitude,
                    startDate = dateStr,
                    endDate = dateStr
                )

                if (omResp.isSuccessful) {
                    val hourly = omResp.body()?.hourly
                    val times = hourly?.time
                    val temps = hourly?.temperature2m
                    val winds = hourly?.windspeed10m
                    val codes = hourly?.weathercode
                    if (!times.isNullOrEmpty() && !temps.isNullOrEmpty()) {
                        val idxExact = times.indexOf(targetHourStr)
                        val idx = if (idxExact >= 0) idxExact else findNearestTimeIndex(times, targetHourStr)
                        if (idx != null && idx in temps.indices) {
                            val tempC = temps[idx]
                            val windKmh = winds?.getOrNull(idx) ?: 0.0
                            val code = codes?.getOrNull(idx) ?: 0
                            val description = mapOpenMeteoCodeToDescription(code)
                            val icon = mapOpenMeteoCodeToIcon(code)
                            result = WeatherInfo(
                                temperature = tempC.roundToInt(),
                                description = description,
                                iconCode = icon,
                                windSpeedKmh = windKmh.roundToInt()
                            )
                        }
                    }
                } else {
                    Log.w(
                        TAG,
                        "Open‑Meteo error: ${omResp.code()} - ${omResp.message()} - ${omResp.errorBody()?.safeString()}"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Open‑Meteo fetch failed: ${e.message}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch historical weather", e)
            null
        }
    }

    private fun findNearestTimeIndex(times: List<String>, target: String): Int? {
        return try {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val zone = java.time.ZoneId.systemDefault()
            val targetDt = java.time.LocalDateTime.parse(target, fmt)
            val targetEpoch = targetDt.atZone(zone).toEpochSecond()
            var bestIdx: Int? = null
            var bestDiff = Long.MAX_VALUE
            times.forEachIndexed { index, t ->
                try {
                    val dt = java.time.LocalDateTime.parse(t, fmt)
                    val epoch = dt.atZone(zone).toEpochSecond()
                    val diff = kotlin.math.abs(epoch - targetEpoch)
                    if (diff < bestDiff) {
                        bestDiff = diff
                        bestIdx = index
                    }
                } catch (_: Exception) {
                }
            }
            bestIdx
        } catch (_: Exception) {
            null
        }
    }

    private fun mapOpenMeteoCodeToDescription(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    private fun mapOpenMeteoCodeToIcon(code: Int): String = when (code) {
        0 -> "01d" // clear
        1, 2 -> "02d" // few clouds
        3 -> "04d" // overcast
        45, 48 -> "50d" // fog
        51, 53, 55, 56, 57 -> "09d" // drizzle
        61, 63, 65 -> "10d" // rain
        66, 67 -> "10d" // freezing rain
        71, 73, 75, 77, 85, 86 -> "13d" // snow
        80, 81, 82 -> "09d" // showers
        95, 96, 99 -> "11d" // thunder
        else -> "01d"
    }
}

private fun ResponseBody.safeString(): String = try {
    string()
} catch (_: Exception) {
    ""
}