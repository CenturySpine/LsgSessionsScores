package com.example.lsgscores.ui.holes

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lsgscores.data.gamezone.GameZone
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.hole.HolePoint
import com.example.lsgscores.ui.common.CombinedPhotoPicker
import com.example.lsgscores.viewmodel.GameZoneViewModel
import com.example.lsgscores.viewmodel.HoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleFormScreen(
    navController: NavHostController,
    holeViewModel: HoleViewModel,
    gameZoneViewModel: GameZoneViewModel
) {

    val gameZones by gameZoneViewModel.gameZones.collectAsState(initial = emptyList())

    var showNameError by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var selectedGameZone by remember { mutableStateOf<GameZone?>(null) }
    var description by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var par by remember { mutableStateOf("") }

    var startName by remember { mutableStateOf("") }
    var endName by remember { mutableStateOf("") }


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
                    text = "Hole name is mandatory",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hole name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // GameZone ComboBox
            Column {
                OutlinedTextField(
                    value = selectedGameZone?.name ?: "",
                    onValueChange = { /* Read-only, selection via dropdown */ },
                    label = { Text("Game Zone") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { gameZoneDropdownExpanded = true },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            "Select Game Zone",
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
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = constraints,
                onValueChange = { constraints = it },
                label = { Text("Constraints") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = distance,
                onValueChange = { distance = it.filter { c -> c.isDigit() } },
                label = { Text("Distance (m)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = par,
                onValueChange = { par = it.filter { c -> c.isDigit() } },
                label = { Text("Par") },
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
                    Text("Start", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = startName,
                        onValueChange = { startName = it },
                        label = { Text("Start name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    CombinedPhotoPicker(
                        onImagePicked = { path -> startPhotoPath = path }
                    )
                }

                // Ending Point (Right)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Target", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endName,
                        onValueChange = { endName = it },
                        label = { Text("Target name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
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
                    Text("Cancel")
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
                                constraints = constraints.takeIf { it.isNotBlank() },
                                distance = distance.toIntOrNull(),
                                par = par.toIntOrNull() ?: 3,
                                start = HolePoint(
                                    name = startName,
                                    photoUri = startPhotoPath
                                ),
                                end = HolePoint(
                                    name = endName,
                                    photoUri = endPhotoPath
                                )
                            )
                            holeViewModel.addHole(hole)
                            navController.popBackStack()
                        }
                    }, // Enable button only if name and a game zone are selected
                    enabled = name.isNotBlank() && selectedGameZone != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}