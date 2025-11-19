package fr.centuryspine.lsgscores.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.centuryspine.lsgscores.data.city.City
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel
import fr.centuryspine.lsgscores.viewmodel.CityViewModel
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionDialog(
    authViewModel: AuthViewModel = hiltViewModel(),
    cityViewModel: CityViewModel = hiltViewModel(),
    onCitySelected: () -> Unit
) {
    val cities by cityViewModel.cities.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Non-dismissible dialog
    Dialog(
        onDismissRequest = { /* Non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sélection de ville",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Veuillez sélectionner votre ville pour continuer",
                    style = MaterialTheme.typography.bodyMedium
                )

                // City dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded && !isCreating }
                ) {
                    OutlinedTextField(
                        value = selectedCity?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ville") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isCreating
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.name) },
                                onClick = {
                                    selectedCity = city
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Validation button
                Button(
                    onClick = {
                        val cityId = selectedCity?.id
                        if (cityId != null) {
                            isCreating = true
                            scope.launch {
                                val success = authViewModel.createPlayerWithCity(cityId)
                                if (success) {
                                    onCitySelected()
                                } else {
                                    // Failed to create player - exit app
                                    (context as? Activity)?.finishAffinity()
                                    exitProcess(0)
                                }
                            }
                        }
                    },
                    enabled = selectedCity != null && !isCreating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isCreating) "Création en cours..." else "Valider")
                }

                if (cities.isEmpty()) {
                    Text(
                        text = "Chargement des villes...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
