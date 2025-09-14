package fr.centuryspine.lsgscores.ui.areas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.city.City
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.viewmodel.CityViewModel
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    gameZoneViewModel: GameZoneViewModel,
    cityViewModel: CityViewModel
) {

    // Cities state
    val cities by cityViewModel.cities.collectAsState(initial = emptyList())
    var showAddCityDialog by remember { mutableStateOf(false) }
    var editingCity by remember { mutableStateOf<City?>(null) }
    var newCityName by remember { mutableStateOf("") }

    // Game zones state
    val gameZones by gameZoneViewModel.gameZones.collectAsState(initial = emptyList())
    var showAddZoneDialog by remember { mutableStateOf(false) }
    var editingGameZone by remember { mutableStateOf<GameZone?>(null) }
    var newZoneName by remember { mutableStateOf("") }
    var gameZoneToDelete by remember { mutableStateOf<GameZone?>(null) }
    var showDeleteZoneDialog by remember { mutableStateOf(false) }

    val gameZoneError by gameZoneViewModel.error.collectAsState()


    Scaffold(
        topBar = {
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Cities section
// Cities section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.areas_section_cities),
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.areas_label_manage_cities),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showAddCityDialog = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_city_content_description)
                        )
                    }
                }

                if (cities.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_cities_defined),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cities.forEach { city ->
                            CityItem(
                                city = city,
                                onEditClick = { editingCity = it }
                            )
                        }
                    }
                }
            }
            // Gaming zones section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.areas_section_gaming_zones),
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_label_manage_game_zones),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showAddZoneDialog = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_game_zone_content_description)
                        )
                    }
                }

                if (gameZones.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_game_zones_defined),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gameZones.forEach { gameZone ->
                            GameZoneItem(
                                gameZone = gameZone,
                                onEditClick = { editingGameZone = it },
                                onDeleteClick = {
                                    gameZoneToDelete = it
                                    showDeleteZoneDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

    }
    if (showAddCityDialog) {
        AlertDialog(
            onDismissRequest = { showAddCityDialog = false },
            title = { Text(stringResource(R.string.add_city_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newCityName,
                    onValueChange = { newCityName = it },
                    label = { Text(stringResource(R.string.city_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    cityViewModel.addCity(newCityName)
                    newCityName = ""
                    showAddCityDialog = false
                }) {
                    Text(stringResource(R.string.add_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newCityName = ""
                    showAddCityDialog = false
                }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Edit city dialog
    editingCity?.let { city ->
        var updatedCityName by remember { mutableStateOf(city.name) }
        AlertDialog(
            onDismissRequest = { editingCity = null },
            title = { Text(stringResource(R.string.edit_city_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = updatedCityName,
                    onValueChange = { updatedCityName = it },
                    label = { Text(stringResource(R.string.city_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    cityViewModel.updateCity(city.copy(name = updatedCityName))
                    editingCity = null
                }) {
                    Text(stringResource(R.string.update_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCity = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (showAddZoneDialog) {
        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false },
            title = { Text(stringResource(R.string.add_game_zone_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newZoneName,
                    onValueChange = { newZoneName = it },
                    label = { Text(stringResource(R.string.game_zone_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    gameZoneViewModel.addGameZone(newZoneName)
                    newZoneName = ""
                    showAddZoneDialog = false
                }) {
                    Text(stringResource(R.string.add_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newZoneName = ""
                    showAddZoneDialog = false
                }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
    // Edit zone dialog
    editingGameZone?.let { gameZone ->
        var updatedZoneName by remember { mutableStateOf(gameZone.name) }
        AlertDialog(
            onDismissRequest = { editingGameZone = null },
            title = { Text(stringResource(R.string.edit_game_zone_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = updatedZoneName,
                    onValueChange = { updatedZoneName = it },
                    label = { Text(stringResource(R.string.game_zone_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    gameZoneViewModel.updateGameZone(gameZone.copy(name = updatedZoneName))
                    editingGameZone = null
                }) {
                    Text(stringResource(R.string.update_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGameZone = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
    
    // Delete game zone confirmation dialog
    if (showDeleteZoneDialog && gameZoneToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteZoneDialog = false
                gameZoneToDelete = null
            },
            title = { Text(stringResource(R.string.delete_game_zone_dialog_title)) },
            text = { Text(stringResource(R.string.delete_game_zone_dialog_message, gameZoneToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    gameZoneViewModel.deleteGameZone(gameZoneToDelete!!)
                    showDeleteZoneDialog = false
                    gameZoneToDelete = null
                }) {
                    Text(stringResource(R.string.delete_game_zone_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteZoneDialog = false
                    gameZoneToDelete = null
                }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
    
    // Error dialog for failed deletion
    gameZoneError?.let { errorMessage ->
        if (errorMessage.contains("holes")) {
            AlertDialog(
                onDismissRequest = { 
                    gameZoneViewModel.clearError()
                },
                title = { Text(stringResource(R.string.delete_game_zone_error_title)) },
                text = { Text(stringResource(R.string.delete_game_zone_error_has_holes)) },
                confirmButton = {
                    TextButton(onClick = { 
                        gameZoneViewModel.clearError()
                    }) {
                        Text(stringResource(R.string.delete_game_zone_ok_button))
                    }
                }
            )
        } else if (errorMessage.contains("sessions")) {
            AlertDialog(
                onDismissRequest = { 
                    gameZoneViewModel.clearError()
                },
                title = { Text(stringResource(R.string.delete_game_zone_error_title)) },
                text = { Text(stringResource(R.string.delete_game_zone_error_has_sessions)) },
                confirmButton = {
                    TextButton(onClick = { 
                        gameZoneViewModel.clearError()
                    }) {
                        Text(stringResource(R.string.delete_game_zone_ok_button))
                    }
                }
            )
        }
    }
}

@Composable
private fun CityItem(
    city: City,
    onEditClick: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = city.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onEditClick(city) }) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_city_content_description))
        }
    }
}

@Composable
fun GameZoneItem(
    gameZone: GameZone,
    onEditClick: (GameZone) -> Unit,
    onDeleteClick: (GameZone) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = gameZone.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Row {
            IconButton(onClick = { onEditClick(gameZone) }) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.edit_game_zone_content_description)
                )
            }
            IconButton(onClick = { onDeleteClick(gameZone) }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.delete_game_zone_content_description),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
