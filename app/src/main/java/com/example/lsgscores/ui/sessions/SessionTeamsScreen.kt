// ui/sessions/SessionTeamsScreen.kt

package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.lsgscores.data.session.SessionType
import com.example.lsgscores.data.player.Player
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel
import com.google.accompanist.flowlayout.FlowRow
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.lsgscores.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTeamsScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    playerViewModel: PlayerViewModel
) {
    val sessionDraft by sessionViewModel.sessionDraft.collectAsState()
    val allPlayers by playerViewModel.players.collectAsState(initial = emptyList())

    // State to track already selected players (so you don't add them in multiple teams)
    var alreadySelectedPlayerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // State for the teams being created
    var teams by remember { mutableStateOf<List<List<Player>>>(emptyList()) }

    // State for currently selected players for the next team
    var currentSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val maxSelectable = if (sessionDraft.sessionType == SessionType.INDIVIDUAL) 1 else 2

    val context = LocalContext.current
    val error by sessionViewModel.error.collectAsState()
    val errorOngoingMessage = stringResource(R.string.session_teams_error_ongoing)

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .padding(bottom = 88.dp), // Space for sticky button
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    stringResource(R.string.session_teams_instruction),
                    style = MaterialTheme.typography.titleMedium
                )

                // Chips selector
                FlowRow(
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    allPlayers.forEach { player ->
                        val isSelectable =
                            (currentSelection.size < maxSelectable || currentSelection.contains(
                                player.id
                            )) &&
                                    !alreadySelectedPlayerIds.contains(player.id)

                        AssistChip(
                            onClick = {
                                if (!isSelectable) return@AssistChip
                                currentSelection = if (currentSelection.contains(player.id)) {
                                    currentSelection - player.id
                                } else {
                                    currentSelection + player.id
                                }
                            },
                            label = { Text(player.name) },
                            leadingIcon = {
                                if (player.photoUri != null) {
                                    AsyncImage(
                                        model = player.photoUri,
                                        contentDescription = player.name,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(player.name.first().toString())
                                }
                            },
                            enabled = isSelectable,
                            colors = if (currentSelection.contains(player.id)) {
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        // Add the new team and reset selection
                        val selectedPlayers = allPlayers.filter { currentSelection.contains(it.id) }
                        if (selectedPlayers.isNotEmpty()) {
                            teams = teams + listOf(selectedPlayers)
                            alreadySelectedPlayerIds = alreadySelectedPlayerIds + currentSelection
                            currentSelection = emptySet()
                        }
                    },
                    enabled = currentSelection.size in 1..maxSelectable
                ) {
                    Text(stringResource(R.string.session_teams_button_add_team))
                }

                // List of teams created so far
                if (teams.isNotEmpty()) {
                    Text(
                        stringResource(R.string.session_teams_label_teams_created),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        teams.forEachIndexed { index, team ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Team label and players
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.session_teams_label_team_prefix,
                                                index + 1
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.width(70.dp)
                                        )
                                        team.forEach { player ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(player.name) },
                                                leadingIcon = {
                                                    if (player.photoUri != null) {
                                                        AsyncImage(
                                                            model = player.photoUri,
                                                            contentDescription = player.name,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Text(player.name.first().toString())
                                                    }
                                                },
                                                enabled = false,
                                                colors = AssistChipDefaults.assistChipColors(
                                                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            // Remove team and make players available again
                                            teams = teams.filterIndexed { i, _ -> i != index }
                                            val playerIdsToRelease = team.map { it.id }.toSet()
                                            alreadySelectedPlayerIds =
                                                alreadySelectedPlayerIds - playerIdsToRelease
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.session_teams_remove_team_description),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.session_teams_button_cancel))
                }

                Button(
                    onClick = {
                        sessionViewModel.startSessionWithTeams(
                            teams = teams.map { team -> team.map { player -> player.id } },
                            onSessionCreated = { sessionId ->
                                navController.navigate("ongoing_session") {
                                    launchSingleTop = true
                                }
                            },
                            onSessionBlocked = {
                                Toast.makeText(
                                    context,
                                    error ?: errorOngoingMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                    enabled = teams.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.session_teams_button_start))
                }
            }
        }
    }
}