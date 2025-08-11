// ui/sessions/SessionCreationScreen.kt

package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.R
import com.example.lsgscores.data.scoring.ScoringMode
import com.example.lsgscores.data.session.SessionType
import com.example.lsgscores.utils.getLocalizedDescription
import com.example.lsgscores.utils.getLocalizedName
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
    var showScoringModeInfo by remember { mutableStateOf(false) }
    var selectedScoringModeForInfo by remember { mutableStateOf<ScoringMode?>(null) }

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
                label = { Text(stringResource(R.string.session_creation_label_date)) },
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Session Type (Individual / Team)
            Text(stringResource(R.string.session_creation_label_session_type))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionType.entries.forEach { type ->
                    OutlinedButton(
                        onClick = { sessionViewModel.setSessionType(type) },
                        border = BorderStroke(
                            2.dp,
                            if (sessionDraft.sessionType == type) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    ) {
                        Text(
                            when (type) {
                                SessionType.INDIVIDUAL -> stringResource(R.string.session_creation_type_individual)
                                SessionType.TEAM -> stringResource(R.string.session_creation_type_team)
                            }
                        )
                    }
                }
            }

            // Scoring Mode
            // Remplacer depuis "// Scoring Mode" jusqu'à la fin du Box par :

// Scoring Mode
            Text(stringResource(R.string.session_creation_label_scoring_mode))

// Grid 2x2 for scoring modes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunkedModes = scoringModes.chunked(2)
                chunkedModes.forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowModes.forEach { mode ->
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sessionDraft.scoringModeId == mode.id,
                                    onClick = { sessionViewModel.setScoringMode(mode.id) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mode.getLocalizedName(LocalContext.current),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedScoringModeForInfo = mode
                                            showScoringModeInfo = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = stringResource(R.string.scoring_mode_info_icon_description),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // Fill remaining space if odd number of items
                        if (rowModes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
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
                    Text(stringResource(R.string.session_creation_button_cancel))
                }
                Button(
                    onClick = {
                        navController.navigate("new_session_teams")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.session_creation_button_next))
                }
            }
        }
    }

    if (showScoringModeInfo && selectedScoringModeForInfo != null) {
        AlertDialog(
            onDismissRequest = { showScoringModeInfo = false },
            title = { Text(stringResource(R.string.scoring_mode_info_title)) },
            text = {
                Column {
                    Text(
                        text = selectedScoringModeForInfo!!.getLocalizedName(LocalContext.current),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedScoringModeForInfo!!.getLocalizedDescription(LocalContext.current))
                }
            },
            confirmButton = {
                TextButton(onClick = { showScoringModeInfo = false }) {
                    Text(stringResource(R.string.ongoing_session_scoring_info_button_ok))
                }
            }
        )
    }
}