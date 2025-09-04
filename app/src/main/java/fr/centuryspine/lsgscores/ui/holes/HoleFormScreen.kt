package fr.centuryspine.lsgscores.ui.holes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.ui.common.CombinedPhotoPicker
import fr.centuryspine.lsgscores.viewmodel.CityViewModel
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.HoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleFormScreen(
    navController: NavHostController,
    holeViewModel: HoleViewModel,
    gameZoneViewModel: GameZoneViewModel,
    cityViewModel: CityViewModel
) {

    val gameZones by gameZoneViewModel.gameZones.collectAsState(initial = emptyList())
    
    val selectedCityId by cityViewModel.selectedCityId.collectAsState()
    
    LaunchedEffect(selectedCityId) {
        gameZoneViewModel.refreshGameZones()
    }

    var showNameError by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var selectedGameZone by remember { mutableStateOf<GameZone?>(null) }
    var description by remember { mutableStateOf("") }

    var distance by remember { mutableStateOf("") }
    var par by remember { mutableStateOf("") }

    var startPhotoPath by remember { mutableStateOf<String?>(null) }
    var endPhotoPath by remember { mutableStateOf<String?>(null) }

    var gameZoneDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (showNameError) {
                Text(
                    text = stringResource(R.string.hole_form_error_name_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.hole_form_label_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // GameZone ComboBox
            Column {
                OutlinedTextField(
                    value = selectedGameZone?.name ?: "",
                    onValueChange = { /* Read-only, selection via dropdown */ },
                    label = { Text(stringResource(R.string.session_creation_label_game_zone)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { gameZoneDropdownExpanded = true },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            stringResource(R.string.session_creation_select_game_zone_placeholder),
                            Modifier.clickable { gameZoneDropdownExpanded = true }
                        )
                    }
                )
                DropdownMenu(
                    expanded = gameZoneDropdownExpanded,
                    onDismissRequest = { gameZoneDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f) // Adjust width as needed
                ) {
                    gameZones.forEach { zone ->
                        DropdownMenuItem(
                            text = { Text(zone.name) },
                            onClick = {
                                selectedGameZone = zone
                                gameZoneDropdownExpanded = false
                            })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.hole_form_label_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = distance,
                onValueChange = { distance = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.hole_form_label_distance)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = par,
                onValueChange = { par = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.hole_form_label_par)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(24.dp))

            // Start and End Points - Row with 2 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Starting Point (Left)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.hole_form_section_start), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    CombinedPhotoPicker(
                        onImagePicked = { path -> startPhotoPath = path }
                    )
                }

                // Ending Point (Right)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.hole_form_section_target), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    CombinedPhotoPicker(
                        onImagePicked = { path -> endPhotoPath = path }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.hole_form_button_cancel))
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            showNameError = true
                        }
                        else if(selectedGameZone == null)
                        {
                            showNameError = true
                        }
                        else {
                            showNameError = false
                            val hole = Hole(
                                name = name,
                                gameZoneId = selectedGameZone!!.id, // Use non-null assertion operator
                                description = description.takeIf { it.isNotBlank() },
                                distance = distance.toIntOrNull(),
                                par = par.toIntOrNull() ?: 3,
                                startPhotoUri =startPhotoPath,
                                endPhotoUri = endPhotoPath
                            )
                            holeViewModel.addHole(hole)
                            navController.popBackStack()
                        }
                    }, // Enable button only if name and a game zone are selected
                    enabled = name.isNotBlank() && selectedGameZone != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.hole_form_button_save))
                }
            }
        }
    }
}