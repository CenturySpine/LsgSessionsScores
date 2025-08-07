package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.holemode.HoleGameModeRepository
import com.example.lsgscores.data.holemode.HoleGameMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngoingSessionScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    holeViewModel: HoleViewModel

) {
    var showHolePicker by remember { mutableStateOf(false) }

    val holes by holeViewModel.holes.collectAsState(initial = emptyList())
    val gameModes by sessionViewModel.holeGameModes.collectAsState()
    var selectedGameModeId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // ... (existing action buttons)

        Button(
            onClick = { showHolePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add or associate a hole")
        }

        if (showHolePicker) {
            var selectedHoleId by remember { mutableStateOf<Long?>(null) }
            var expanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showHolePicker = false },
                title = { Text("Select or create a hole") },
                text = {
                    Column {
                        // Hole selection dropdown
                        Text("Select a hole")
                        Spacer(Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = holes.find { it.id == selectedHoleId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hole") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                holes.forEach { hole ->
                                    DropdownMenuItem(
                                        text = { Text(hole.name) },
                                        onClick = {
                                            selectedHoleId = hole.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Mode selection
                        Text("Select game mode")
                        Spacer(Modifier.height(8.dp))
                        gameModes.forEach { mode ->
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedGameModeId == mode.id,
                                    onClick = { selectedGameModeId = mode.id }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(mode.name)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        // Option to create a new hole
                        Button(
                            onClick = {
                                showHolePicker = false
                                navController.navigate("add_hole")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create a new hole")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedHoleId != null && selectedGameModeId != null) {
                                sessionViewModel.addPlayedHole(
                                    holeId = selectedHoleId!!,
                                    gameModeId = selectedGameModeId!!,
                                    onPlayedHoleCreated = { playedHoleId ->
                                        // Navigate to the score entry screen for the new played hole
                                        navController.navigate("played_hole_score/$playedHoleId")
                                        showHolePicker = false
                                        selectedHoleId = null
                                        selectedGameModeId = null
                                    }
                                )                            }
                        },
                        enabled = selectedHoleId != null && selectedGameModeId != null
                    ) {
                        Text("Add to session")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHolePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ... (rest of your UI)
    }
}
