package fr.centuryspine.lsgscores.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lsgscores.data.weather.WeatherInfo

/**
 * Small reusable weather summary: icon + temperature + wind speed.
 * This mirrors the inline rendering used in SessionHistoryCard.
 * No hard-coded labels other than units, consistent with existing UI.
 */
@Composable
fun WeatherSummaryRow(
    weatherInfo: WeatherInfo,
    iconSize: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherIcon(weatherInfo = weatherInfo, size = iconSize)
        Column {
            Text(
                text = "${weatherInfo.temperature}°C",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${weatherInfo.windSpeedKmh} km/h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
