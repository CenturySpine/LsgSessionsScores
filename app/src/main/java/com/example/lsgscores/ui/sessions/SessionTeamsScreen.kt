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
import com.example.lsgscores.data.user.User
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.UserViewModel
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTeamsScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    userViewModel: UserViewModel
) {
    val sessionDraft by sessionViewModel.sessionDraft.collectAsState()
    val allUsers by userViewModel.users.collectAsState(initial = emptyList<User>())

    // State to track already selected users (so you don't add them in multiple teams)
    var alreadySelectedUserIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // State for the teams being created
    var teams by remember { mutableStateOf<List<List<User>>>(emptyList()) }

    // State for currently selected users for the next team
    var currentSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val maxSelectable = if (sessionDraft.sessionType == SessionType.INDIVIDUAL) 1 else 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Teams") },
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
            Text(
                "Select players to compose a team",
                style = MaterialTheme.typography.titleMedium
            )

            // Chips selector
            FlowRow(
                mainAxisSpacing  = 8.dp,
                crossAxisSpacing  = 8.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                allUsers.forEach { user ->
                    val isSelectable =
                        (currentSelection.size < maxSelectable || currentSelection.contains(user.id)) &&
                                !alreadySelectedUserIds.contains(user.id)

                    AssistChip(
                        onClick = {
                            if (!isSelectable) return@AssistChip
                            currentSelection = if (currentSelection.contains(user.id)) {
                                currentSelection - user.id
                            } else {
                                currentSelection + user.id
                            }
                        },
                        label = { Text(user.name) },
                        leadingIcon = {
                            if (user.photoUri != null) {
                                AsyncImage(
                                    model = user.photoUri,
                                    contentDescription = user.name,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(user.name.first().toString())
                            }
                        },
                        enabled = isSelectable,
                        colors = if (currentSelection.contains(user.id)) {
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
                    val selectedUsers = allUsers.filter { currentSelection.contains(it.id) }
                    if (selectedUsers.isNotEmpty()) {
                        teams = teams + listOf(selectedUsers)
                        alreadySelectedUserIds = alreadySelectedUserIds + currentSelection
                        currentSelection = emptySet()
                    }
                },
                enabled = currentSelection.size in 1..maxSelectable
            ) {
                Text("Add team")
            }

            // List of teams created so far
            if (teams.isNotEmpty()) {
                Text("Teams created:", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    teams.forEachIndexed { index, team ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Team ${index + 1}:")
                            team.forEach { user ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(user.name) },
                                    leadingIcon = {
                                        if (user.photoUri != null) {
                                            AsyncImage(
                                                model = user.photoUri,
                                                contentDescription = user.name,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(user.name.first().toString())
                                        }
                                    },
                                    enabled = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // TODO: Save teams and session, then navigate to the next step or go back
                },
                enabled = teams.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start session")
            }
        }
    }
}
