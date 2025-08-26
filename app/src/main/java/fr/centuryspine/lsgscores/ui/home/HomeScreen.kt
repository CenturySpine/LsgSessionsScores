package fr.centuryspine.lsgscores.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.centuryspine.lsgscores.R

@Composable
fun HomeScreen() {
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

        }
    }

}
