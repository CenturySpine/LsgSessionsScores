package fr.centuryspine.lsgscores.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.holemode.HoleGameMode
import fr.centuryspine.lsgscores.data.session.SessionType
import fr.centuryspine.lsgscores.ui.BottomNavItem
import fr.centuryspine.lsgscores.ui.DrawerNavItem
import fr.centuryspine.lsgscores.utils.getLocalizedDescription
import fr.centuryspine.lsgscores.utils.getLocalizedName
import fr.centuryspine.lsgscores.viewmodel.HoleViewModel
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import fr.centuryspine.lsgscores.utils.getLocalizedDescription as getGameModeLocalizedDescription

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngoingSessionScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    holeViewModel: HoleViewModel
) {
    val isParticipant by sessionViewModel.isParticipantMode.collectAsState()
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
    var playedHoleToDelete by remember { mutableStateOf<Long?>(null) }
    var showDeletePlayedHoleConfirm by remember { mutableStateOf(false) }
    var showValidateConfirm by remember { mutableStateOf(false) }
    var showGameModeInfo by remember { mutableStateOf(false) }
    var selectedGameModeForInfo by remember { mutableStateOf<HoleGameMode?>(null) }

    // Teams of the session to detect missing scores
    val teamsForSession by ((ongoingSession?.let { sessionViewModel.getTeamsWithPlayersForSession(it.id) } ?: flowOf(emptyList())))
        .collectAsState(initial = emptyList())

    val hasMissingScores = ongoingSession != null && playedHoles.any { it.teamResults.size < teamsForSession.size }

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
            if (ongoingSession == null) {
                Text(
                    text = stringResource(R.string.ongoing_session_no_holes_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
                            text = stringResource(
                                R.string.ongoing_session_date_format,
                                session.dateTime.format(
                                    DateTimeFormatter.ofPattern(
                                        "dd MMMM yyyy",
                                        Locale.FRENCH
                                    )
                                )
                            ),
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
                                text = scoringMode.getLocalizedName(LocalContext.current),
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
                                    contentDescription = stringResource(R.string.ongoing_session_scoring_info_icon_description),
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
                    if (hasMissingScores) {
                        Text(
                            text = "Classement provisoire: des scores manquants",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    StandingsTable(standings = teamStandings)
                }
                if (!isParticipant) {
                    OutlinedButton(
                        onClick = { navController.navigate("session_qr") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Afficher le QR de la session")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showHolePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ongoing_session_button_add_hole))
                    }
                }
                // Section des trous joués
                if (playedHoles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ongoing_session_no_holes_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ongoing_session_label_holes_played),
                        style = MaterialTheme.typography.titleMedium
                    )

                    playedHoles.forEach { playedHole ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("played_hole_score/${playedHole.playedHoleId}") },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.ongoing_session_hole_prefix)} ${playedHole.holeName} (${playedHole.gameModeName})",
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

                                if (!isParticipant) {
                                    IconButton(
                                        onClick = {
                                            playedHoleToDelete = playedHole.playedHoleId
                                            showDeletePlayedHoleConfirm = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.ongoing_session_delete_hole_icon_description),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    // No delete action in participant mode
                                }
                                }
                            }
                        }
                    }



                if (showHolePicker) {
                    var selectedHoleId by remember { mutableStateOf<Long?>(null) }
                    var expanded by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showHolePicker = false },
                        title = { Text(stringResource(R.string.ongoing_session_picker_title)) },
                        text = {
                            Column {
                                // Hole selection dropdown
                                Text(stringResource(R.string.ongoing_session_picker_label_select_hole))
                                Spacer(Modifier.height(8.dp))
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = holes.find { it.id == selectedHoleId }?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.ongoing_session_picker_label_hole)) },
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
                                Text(stringResource(R.string.ongoing_session_picker_label_game_mode))
                                Spacer(Modifier.height(8.dp))


                                val filteredGameModes = when (session.sessionType) {
                                    SessionType.INDIVIDUAL -> {
                                        gameModes.filter { it.id == 1 } // Only Individual mode (id = 1)
                                    }

                                    SessionType.TEAM -> {
                                        gameModes.filter { it.id != 1 } // All modes except Individual
                                    }
                                }


                                if (selectedGameModeId == null) {
                                    selectedGameModeId = when (session.sessionType) {
                                        SessionType.INDIVIDUAL -> 1 // Individual
                                        SessionType.TEAM -> 2 // Scramble
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
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(mode.name)
                                            IconButton(
                                                onClick = {
                                                    selectedGameModeForInfo = mode
                                                    showGameModeInfo = true
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = stringResource(R.string.game_mode_info_title),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
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
                                    Text(stringResource(R.string.ongoing_session_picker_button_create_hole))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            showHolePicker = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.ongoing_session_picker_button_cancel))
                                    }

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
                                        enabled = selectedHoleId != null && selectedGameModeId != null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.ongoing_session_picker_button_add))
                                    }
                                }

                            }
                        },
                        confirmButton = { },
                        dismissButton = { }
                    )
                }
            }
        }

        // Sticky buttons at bottom
        if (!isParticipant) {
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
                    Text(stringResource(R.string.ongoing_session_button_cancel))
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = { showValidateConfirm = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ongoing_session_button_validate))
                }
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.ongoing_session_delete_dialog_title)) },
            text = {
                Text(stringResource(R.string.ongoing_session_delete_dialog_message))
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
                ) { Text(stringResource(R.string.ongoing_session_delete_dialog_button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                }) { Text(stringResource(R.string.ongoing_session_delete_dialog_button_cancel)) }
            }
        )
    }


    if (showScoringModeInfo) {
        currentScoringMode?.let { scoringMode ->
            AlertDialog(
                onDismissRequest = { showScoringModeInfo = false },
                title = { Text(scoringMode.getLocalizedName(LocalContext.current)) },
                text = { Text(scoringMode.getLocalizedDescription(LocalContext.current)) },
                confirmButton = {
                    TextButton(onClick = { showScoringModeInfo = false }) {
                        Text(stringResource(R.string.ongoing_session_scoring_info_button_ok))
                    }
                }
            )
        }
    }

    // Delete played hole confirmation dialog
    if (showDeletePlayedHoleConfirm && playedHoleToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeletePlayedHoleConfirm = false
                playedHoleToDelete = null
            },
            title = { Text(stringResource(R.string.ongoing_session_delete_hole_dialog_title)) },
            text = {
                Text(stringResource(R.string.ongoing_session_delete_hole_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playedHoleToDelete?.let { playedHoleId ->
                            sessionViewModel.deletePlayedHole(playedHoleId)
                        }
                        showDeletePlayedHoleConfirm = false
                        playedHoleToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.ongoing_session_delete_hole_dialog_button_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeletePlayedHoleConfirm = false
                        playedHoleToDelete = null
                    }
                ) { Text(stringResource(R.string.ongoing_session_delete_hole_dialog_button_cancel)) }
            }
        )
    }
    // Validate session confirmation dialog
    if (showValidateConfirm) {
        AlertDialog(
            onDismissRequest = { showValidateConfirm = false },
            title = { Text(stringResource(R.string.ongoing_session_validate_dialog_title)) },
            text = {
                Text(stringResource(R.string.ongoing_session_validate_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionViewModel.validateOngoingSession {
                            // Navigate to history after validation
                            navController.navigate(DrawerNavItem.SessionHistory.route) {
                                popUpTo(BottomNavItem.Home.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                        showValidateConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.ongoing_session_validate_dialog_button_complete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showValidateConfirm = false }) {
                    Text(stringResource(R.string.ongoing_session_validate_dialog_button_cancel))
                }
            }
        )
    }

    // Game mode info dialog
    if (showGameModeInfo && selectedGameModeForInfo != null) {
        AlertDialog(
            onDismissRequest = { showGameModeInfo = false },
            title = { Text(stringResource(R.string.game_mode_info_title)) },
            text = {
                Column {
                    Text(
                        text = selectedGameModeForInfo!!.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedGameModeForInfo!!.getGameModeLocalizedDescription(LocalContext.current))
                }
            },
            confirmButton = {
                TextButton(onClick = { showGameModeInfo = false }) {
                    Text(stringResource(R.string.ongoing_session_scoring_info_button_ok))
                }
            }
        )
    }
}