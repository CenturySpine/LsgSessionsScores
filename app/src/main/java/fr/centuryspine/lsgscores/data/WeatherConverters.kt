package fr.centuryspine.lsgscores.data

import androidx.room.TypeConverter
import com.example.lsgscores.data.weather.WeatherInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database to handle WeatherInfo serialization
 */
class WeatherConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromWeatherInfo(weatherInfo: WeatherInfo?): String? {
        return weatherInfo?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toWeatherInfo(weatherData: String?): WeatherInfo? {
        return weatherData?.let {
            val type = object : TypeToken<WeatherInfo>() {}.type
            gson.fromJson(it, type)
        }
    }
}