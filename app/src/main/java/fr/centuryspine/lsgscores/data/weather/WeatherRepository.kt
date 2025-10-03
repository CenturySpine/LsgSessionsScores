package com.example.lsgscores.data.weather

import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class WeatherRepository(private val apiKey: String) {

    companion object {
        private const val BASE_URL_V2 = "https://api.openweathermap.org/data/2.5/"
        private const val BASE_URL_V3 = "https://api.openweathermap.org/data/3.0/"
        private const val BASE_URL_HISTORY = "https://history.openweathermap.org/data/2.5/"
        private const val BASE_URL_OPEN_METEO = "https://archive-api.open-meteo.com/"
        private const val BASE_URL_OPEN_METEO_FORECAST = "https://api.open-meteo.com/"
        private const val TAG = "WeatherRepository"
    }

    private val serviceV2: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_V2)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }

    private val serviceV3: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_V3)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }

    private val serviceHistory: HistoryWeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_HISTORY)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HistoryWeatherService::class.java)
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
                Log.e(TAG, "Open‑Meteo current weather error: ${response.code()} - ${response.message()} - ${response.errorBody()?.safeString()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch current weather (Open‑Meteo)", e)
            null
        }
    }

    suspend fun getHistoricalWeather(latitude: Double, longitude: Double, unixTimestamp: Long): WeatherInfo? {
        return try {
            // 1) Try Open‑Meteo Archive API first (free, no key required)
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
                            return WeatherInfo(
                                temperature = tempC.roundToInt(),
                                description = description,
                                iconCode = icon,
                                windSpeedKmh = windKmh.roundToInt()
                            )
                        }
                    }
                } else {
                    Log.w(TAG, "Open‑Meteo error: ${omResp.code()} - ${omResp.message()} - ${omResp.errorBody()?.safeString()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Open‑Meteo fetch failed: ${e.message}")
            }

            // 2) Fallback: OpenWeather History host
            val historyResp = serviceHistory.getCityHistory(
                latitude = latitude,
                longitude = longitude,
                start = unixTimestamp,
                end = unixTimestamp,
                apiKey = apiKey
            )
            if (historyResp.isSuccessful) {
                val list = historyResp.body()?.list.orEmpty()
                val item = list.minByOrNull { kotlin.math.abs((it.dt ?: unixTimestamp) - unixTimestamp) }
                val main = item?.main
                val wind = item?.wind
                val w = item?.weather?.firstOrNull()
                if (main != null) {
                    return WeatherInfo(
                        temperature = main.temp.roundToInt(),
                        description = w?.description ?: "Unknown",
                        iconCode = w?.icon ?: "01d",
                        windSpeedKmh = ((wind?.speed ?: 0.0) * 3.6).roundToInt()
                    )
                }
            } else {
                Log.e(TAG, "History API error: ${historyResp.code()} - ${historyResp.message()} - ${historyResp.errorBody()?.safeString()}")
                if (historyResp.code() == 401) {
                    Log.e(TAG, "401 Unauthorized from History API: Key/plan may not include history access. See https://openweathermap.org/history.")
                }
            }

            // 3) Fallback: One Call 3.0 Time Machine
            val responseV3 = serviceV3.getHistoricalWeather(latitude, longitude, unixTimestamp, apiKey)
            if (responseV3.isSuccessful) {
                parseHistoricalBody(responseV3.body())
            } else {
                val code = responseV3.code()
                val msg = responseV3.message()
                val bodyStr = responseV3.errorBody()?.safeString()
                Log.e(TAG, "Historical Weather API v3 error: ${code} - ${msg} - ${bodyStr}")
                if (code == 401) {
                    Log.e(TAG, "401 Unauthorized from One Call 3.0: your API key/plan likely doesn't include historical data. See https://openweathermap.org/api/one-call-3 for plan requirements.")
                }
                // 4) Fallback: try legacy v2.5 endpoint (may be unavailable or deprecated)
                val responseV2 = serviceV2.getHistoricalWeather(latitude, longitude, unixTimestamp, apiKey)
                if (responseV2.isSuccessful) {
                    parseHistoricalBody(responseV2.body())
                } else {
                    Log.e(TAG, "Historical Weather API v2.5 error: ${responseV2.code()} - ${responseV2.message()} - ${responseV2.errorBody()?.safeString()}")
                    if (responseV2.code() == 401) {
                        Log.e(TAG, "401 Unauthorized from One Call 2.5: Most free keys are not authorized for Time Machine. You may need a different API key/plan (One Call 3.0, History API).")
                    }
                    null
                }
            }
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
                } catch (_: Exception) { }
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

    private fun parseHistoricalBody(body: OneCallTimemachineResponse?): WeatherInfo? {
        val current = body?.current ?: return null
        val temp = current.temp ?: return null
        val windSpeed = current.windSpeed ?: 0.0
        val firstWeather = current.weather?.firstOrNull()
        return WeatherInfo(
            temperature = temp.roundToInt(),
            description = firstWeather?.description ?: "Unknown",
            iconCode = firstWeather?.icon ?: "01d",
            windSpeedKmh = (windSpeed * 3.6).roundToInt()
        )
    }
}

private fun ResponseBody.safeString(): String = try { string() } catch (_: Exception) { "" }