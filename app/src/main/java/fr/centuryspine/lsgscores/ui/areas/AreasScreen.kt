package fr.centuryspine.lsgscores.ui.areas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.centuryspine.lsgscores.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    gameZoneViewModel: GameZoneViewModel
) {
    val gameZones by gameZoneViewModel.gameZones.collectAsState(initial = emptyList())
    var showAddZoneDialog by remember { mutableStateOf(false) }
    var editingGameZone by remember { mutableStateOf<GameZone?>(null) }
    var newZoneName by remember { mutableStateOf("") }

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.areas_section_cities),
                    style = MaterialTheme.typography.titleLarge
                )

                // TODO: Cities list will go here
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
                                onEditClick = { editingGameZone = it }
                            )
                        }
                    }
                }
            }
        }

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
}

@Composable
fun GameZoneItem(
    gameZone: GameZone,
    onEditClick: (GameZone) -> Unit,
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
        IconButton(onClick = { onEditClick(gameZone) }) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.edit_game_zone_content_description)
            )
        }
    }
}
