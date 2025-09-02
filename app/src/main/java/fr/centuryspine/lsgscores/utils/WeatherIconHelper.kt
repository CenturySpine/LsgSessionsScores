package fr.centuryspine.lsgscores.utils

import com.example.lsgscores.data.weather.WeatherInfo

/**
 * Helper object for weather icon URLs from OpenWeatherMap
 */
object WeatherIconHelper {

    /**
     * Base URL for OpenWeatherMap icons
     * Available sizes: @1x (50x50), @2x (100x100), @4x (200x200)
     */
    private const val ICON_BASE_URL = "https://openweathermap.org/img/wn/"

    /**
     * Get the icon URL for a weather info object
     * @param weatherInfo The weather info containing the icon code
     * @param size The size multiplier (1, 2, or 4). Default is 2 for 100x100 pixels
     * @return The complete URL to the weather icon, or null if no icon code
     */
    fun getIconUrl(weatherInfo: WeatherInfo?, size: Int = 2): String? {
        return weatherInfo?.iconCode?.let { code ->
            "${ICON_BASE_URL}${code}@${size}x.png"
        }
    }

    /**
     * Get a small icon URL (50x50)
     */
    fun getSmallIconUrl(weatherInfo: WeatherInfo?): String? =
        getIconUrl(weatherInfo, 1)

    /**
     * Get a medium icon URL (100x100) - default
     */
    fun getMediumIconUrl(weatherInfo: WeatherInfo?): String? =
        getIconUrl(weatherInfo, 2)

    /**
     * Get a large icon URL (200x200)
     */
    fun getLargeIconUrl(weatherInfo: WeatherInfo?): String? =
        getIconUrl(weatherInfo, 4)
}