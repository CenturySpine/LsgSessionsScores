// ui/sessions/SessionCreationScreen.kt

package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date (non-editable)
            val formattedDate =
                sessionDraft.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
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
                    val modeName =
                        scoringModes.find { it.id == sessionDraft.scoringModeId }?.name ?: "Choose"
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

            // Buttons row
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
                        navController.navigate("new_session_teams")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
            }
        }
    }
}
