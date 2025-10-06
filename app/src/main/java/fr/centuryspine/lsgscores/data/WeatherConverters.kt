package fr.centuryspine.lsgscores.data

import com.example.lsgscores.data.weather.WeatherInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Legacy Room TypeConverters removed; keep plain serialization helpers.
class WeatherConverters {

    private val gson = Gson()

    fun fromWeatherInfo(weatherInfo: WeatherInfo?): String? {
        return weatherInfo?.let { gson.toJson(it) }
    }

    fun toWeatherInfo(weatherData: String?): WeatherInfo? {
        return weatherData?.let {
            val type = object : TypeToken<WeatherInfo>() {}.type
            gson.fromJson(it, type)
        }
    }
}