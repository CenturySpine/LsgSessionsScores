package fr.centuryspine.lsgscores.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lsgscores.data.weather.WeatherInfo
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.utils.WeatherIconHelper

/**
 * Composable to display a weather icon from OpenWeatherMap
 *
 * @param weatherInfo The weather information containing the icon code
 * @param size The size of the icon to display
 * @param modifier Additional modifiers
 */
@Composable
fun WeatherIcon(
    weatherInfo: WeatherInfo?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    weatherInfo?.let { weather ->
        val iconUrl = WeatherIconHelper.getMediumIconUrl(weather)

        if (iconUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = weather.description,
                modifier = modifier.size(size),
                contentScale = ContentScale.Fit,
                // Use a simple gray circle as placeholder
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                // Fallback icon if loading fails
                error = painterResource(id = android.R.drawable.ic_dialog_info)
            )
        }
    }
}

/**
 * Small weather icon (24dp)
 */
@Composable
fun SmallWeatherIcon(
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier
) {
    WeatherIcon(weatherInfo, 24.dp, modifier)
}

/**
 * Large weather icon (64dp)
 */
@Composable
fun LargeWeatherIcon(
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier
) {
    WeatherIcon(weatherInfo, 64.dp, modifier)
}