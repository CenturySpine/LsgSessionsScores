package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.ui.BottomNavItem
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngoingSessionScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    holeViewModel: HoleViewModel
) {
    var showHolePicker by remember { mutableStateOf(false) }
    val ongoingSession = sessionViewModel.ongoingSession.collectAsState(initial = null).value
    val holes by holeViewModel.holes.collectAsState(initial = emptyList())
    val gameModes by sessionViewModel.holeGameModes.collectAsState()
    val playedHoles by sessionViewModel.playedHolesWithScores.collectAsState()
    var selectedGameModeId by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val teamStandings by sessionViewModel.teamStandings.collectAsState()
    val currentScoringMode by sessionViewModel.currentScoringModeInfo.collectAsState()
    var showScoringModeInfo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 88.dp), // Space for sticky buttons
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ongoingSession?.let { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Session du ${
                                session.dateTime.format(
                                    DateTimeFormatter.ofPattern(
                                        "dd MMMM yyyy",
                                        Locale.FRENCH
                                    )
                                )
                            }",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Carte du mode de scoring
                currentScoringMode?.let { scoringMode ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scoringMode.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showScoringModeInfo = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Scoring mode info",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Team standings table (only show if we have data)
                if (teamStandings.isNotEmpty()) {
                    StandingsTable(standings = teamStandings)
                }
                Button(
                    onClick = { showHolePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add or associate a hole")
                }
                // Section des trous joués
                if (playedHoles.isEmpty()) {
                    Text(
                        text = "No holes have been played yet. Press the button below to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "Holes played:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    playedHoles.forEach { playedHole ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Hole: ${playedHole.holeName} (${playedHole.gameModeName})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = playedHole.teamResults.joinToString(", ") {
                                        "${it.teamName}: ${it.strokes} - ${it.calculatedScore}"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
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
                                        modifier = Modifier
                                            .menuAnchor(
                                                MenuAnchorType.PrimaryNotEditable,
                                                enabled = true
                                            )
                                            .fillMaxWidth()
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

// Filter game modes based on session type
                                val filteredGameModes = when (session.sessionType) {
                                    com.example.lsgscores.data.session.SessionType.INDIVIDUAL -> {
                                        gameModes.filter { it.id == 1 } // Only Individual mode (id = 1)
                                    }

                                    com.example.lsgscores.data.session.SessionType.TEAM -> {
                                        gameModes.filter { it.id != 1 } // All modes except Individual
                                    }
                                }

// Auto-select default mode if none selected
                                if (selectedGameModeId == null) {
                                    selectedGameModeId = when (session.sessionType) {
                                        com.example.lsgscores.data.session.SessionType.INDIVIDUAL -> 1 // Individual
                                        com.example.lsgscores.data.session.SessionType.TEAM -> 2 // Scramble
                                    }
                                }

                                filteredGameModes.forEach { mode ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
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
                                        )
                                    }
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
            }
        }

        // Sticky buttons at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(24.dp))
            Button(
                onClick = { /* TODO: Validate later */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Validate")
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete session") },
            text = {
                Text(
                    "This will permanently delete the session and all its related data:\n" +
                            "• Teams\n• Played holes\n• Scores\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ongoingSession?.let { session ->
                            sessionViewModel.deleteSessionAndAllData(session) {
                                navController.navigate(BottomNavItem.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        showDeleteConfirm = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }


    if (showScoringModeInfo) {
        currentScoringMode?.let { scoringMode ->
            AlertDialog(
                onDismissRequest = { showScoringModeInfo = false },
                title = { Text(scoringMode.name) },
                text = { Text(scoringMode.description) },
                confirmButton = {
                    TextButton(onClick = { showScoringModeInfo = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}