package fr.centuryspine.lsgscores.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
    var showNewCityInput by remember { mutableStateOf(false) }
    var newCityName by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
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

                // City dropdown with edit icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded && !isCreating && !showNewCityInput },
                        modifier = Modifier.weight(1f)
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
                            enabled = !isCreating && !showNewCityInput
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

                    IconButton(
                        onClick = {
                            showNewCityInput = !showNewCityInput
                            if (showNewCityInput) {
                                selectedCity = null
                                newCityName = ""
                            }
                        },
                        enabled = !isCreating
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Créer une nouvelle ville"
                        )
                    }
                }

                // New city input field (conditionally visible)
                if (showNewCityInput) {
                    OutlinedTextField(
                        value = newCityName,
                        onValueChange = { newCityName = it },
                        label = { Text("Nouvelle ville") },
                        placeholder = { Text("Nom de la ville") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreating,
                        singleLine = true
                    )
                }

                // Validation button
                Button(
                    onClick = {
                        if (showNewCityInput && newCityName.isNotBlank()) {
                            // Show confirmation dialog for new city
                            showConfirmationDialog = true
                        } else {
                            val cityId = selectedCity?.id
                            if (cityId != null) {
                                isCreating = true
                                scope.launch {
                                    val success = authViewModel.createPlayerWithCity(cityId)
                                    if (success) {
                                        cityViewModel.loadAuthenticatedUserCity()
                                        onCitySelected()

                                    } else {
                                        // Failed to create player - exit app
                                        (context as? Activity)?.finishAffinity()
                                        exitProcess(0)
                                    }
                                }
                            }
                        }
                    },
                    enabled = (selectedCity != null || (showNewCityInput && newCityName.isNotBlank())) && !isCreating,
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

    // Confirmation dialog for new city creation
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreating) showConfirmationDialog = false },
            title = { Text("Confirmer la création de ville") },
            text = { Text("Voulez-vous créer la ville \"$newCityName\" ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isCreating = true
                        scope.launch {
                            // Create the city
                            val createdCity = cityViewModel.addCity(newCityName)
                            if (createdCity != null) {
                                // City created successfully, now create the player
                                val success = authViewModel.createPlayerWithCity(createdCity.id)
                                if (success) {
                                    cityViewModel.loadAuthenticatedUserCity()
                                    onCitySelected()
                                } else {
                                    // Failed to create player - exit app
                                    (context as? Activity)?.finishAffinity()
                                    exitProcess(0)
                                }
                            } else {
                                // Failed to create city - exit app
                                (context as? Activity)?.finishAffinity()
                                exitProcess(0)
                            }
                        }
                    },
                    enabled = !isCreating
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false },
                    enabled = !isCreating
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}
