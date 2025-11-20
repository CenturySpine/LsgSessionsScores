package fr.centuryspine.lsgscores.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.viewmodel.CityViewModel

@Composable
fun HomeScreen(
    cityViewModel: CityViewModel,
    onJoinSessionClick: (() -> Unit)? = null
) {
    val authenticatedUserCityName by cityViewModel.authenticatedUserCityName.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {



            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(300.dp)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    AsyncImage(
                        model = R.drawable.lsg_vert_banner,
                        contentDescription = stringResource(R.string.home_logo_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.home_welcome_text),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Display city name for authenticated user's player
            if (authenticatedUserCityName != null) {
                Text(
                    text = authenticatedUserCityName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = stringResource(R.string.home_no_city_selected),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = { onJoinSessionClick?.invoke() }) {
                Text("Rejoindre une session")
            }
        }
    }
}