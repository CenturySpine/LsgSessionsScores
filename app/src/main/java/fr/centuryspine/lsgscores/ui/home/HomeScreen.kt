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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.viewmodel.CityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cityViewModel: CityViewModel
) {
    val cities by cityViewModel.cities.collectAsState(initial = emptyList())
    val selectedCityId by cityViewModel.selectedCityId.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val selectedCity = cities.find { it.id == selectedCityId }

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

            // City selection dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.width(250.dp)
            ) {
                OutlinedTextField(
                    value = selectedCity?.name ?: stringResource(R.string.home_no_city_selected),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.home_select_city)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    cities.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.name) },
                            onClick = {
                                cityViewModel.selectCity(city.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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