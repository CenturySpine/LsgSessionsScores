// ui/sessions/SessionCreationScreen.kt

package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.data.scoring.ScoringMode
import com.example.lsgscores.data.session.SessionType
import com.example.lsgscores.viewmodel.SessionViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCreationScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel
) {
    val sessionDraft by sessionViewModel.sessionDraft.collectAsState()
    val scoringModes by sessionViewModel.scoringModes.collectAsState()

    // For the dropdown scoring mode
    var scoringDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Session") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Create a new session", style = MaterialTheme.typography.titleLarge)

            // Date (non-editable)
            val formattedDate = sessionDraft.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            OutlinedTextField(
                value = formattedDate,
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Session Type (Individual / Team)
            Text("Session type")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionType.values().forEach { type ->
                    OutlinedButton(
                        onClick = { sessionViewModel.setSessionType(type) },
                        border = BorderStroke(
                            2.dp,
                            if (sessionDraft.sessionType == type) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    ) {
                        Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Scoring Mode
            Text("Scoring mode")
            Box {
                OutlinedButton(
                    onClick = { scoringDropdownExpanded = true }
                ) {
                    val modeName = scoringModes.find { it.id == sessionDraft.scoringModeId }?.name ?: "Choose"
                    Text(modeName)
                }
                DropdownMenu(
                    expanded = scoringDropdownExpanded,
                    onDismissRequest = { scoringDropdownExpanded = false }
                ) {
                    scoringModes.forEach { mode ->
                        DropdownMenuItem(
                            onClick = {
                                sessionViewModel.setScoringMode(mode.id)
                                scoringDropdownExpanded = false
                            },
                            text = { Text(mode.name) }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Next button to go to teams selection
            Button(
                onClick = {
                    navController.navigate("new_session_teams")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}
