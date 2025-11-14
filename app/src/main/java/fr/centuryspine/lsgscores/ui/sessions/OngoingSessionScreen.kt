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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import android.widget.Toast
import android.util.Log
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import fr.centuryspine.lsgscores.ui.common.RemoteImage
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
    // Affichage du classement: replié par défaut
    var standingsExpanded by remember { mutableStateOf(false) }


    // Participant: detect session end (validated or deleted) via ViewModel events and redirect with a toast
    val context = LocalContext.current
    var hasSeenActiveSession by remember { mutableStateOf(false) }
    var sessionEndHandled by remember { mutableStateOf(false) }
    var endDialogReason by remember { mutableStateOf<SessionViewModel.SessionEvent.EndReason?>(null) }

    // Track when we saw an active session to avoid reacting on first load
    LaunchedEffect(ongoingSession?.isOngoing) {
        if (isParticipant && ongoingSession?.isOngoing == true) {
            hasSeenActiveSession = true
            sessionEndHandled = false
        }
    }

    // React to explicit end events from ViewModel (robust against transient nulls)
    LaunchedEffect(isParticipant) {
        if (isParticipant) {
            sessionViewModel.sessionEvents.collect { evt ->
                // Do not gate on hasSeenActiveSession: events are explicit (VALIDATED/DELETED)
                if (!sessionEndHandled && evt is SessionViewModel.SessionEvent.Ended) {
                    endDialogReason = evt.reason
                    sessionEndHandled = true
                }
            }
        }
    }

    // Participant: on session end, show a toast and return to Home automatically
    LaunchedEffect(endDialogReason) {
        endDialogReason?.let { reason ->
            val messageRes = when (reason) {
                SessionViewModel.SessionEvent.EndReason.DELETED -> R.string.ongoing_session_deleted_message
                SessionViewModel.SessionEvent.EndReason.VALIDATED -> R.string.ongoing_session_finished_message
                else -> R.string.ongoing_session_closed_dialog_message
            }
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_LONG).show()
            // Reset participant state and navigate back home
            sessionViewModel.setParticipantTeam(null)
            sessionViewModel.setParticipantSession(null)
            sessionViewModel.setParticipantMode(false)
            navController.navigate(BottomNavItem.Home.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            endDialogReason = null
        }
    }

    // Teams of the session to detect missing scores
    val teamsForSession by ((ongoingSession?.let {
        sessionViewModel.getTeamsWithPlayersForSession(it.id) } ?: flowOf(emptyList())))
        .collectAsState(initial = emptyList())
    val effectiveTeamId by sessionViewModel.effectiveUserTeamId.collectAsState()

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
                Log.d("OngoingSessionScreen", "session = null")
                Text(
                    text = stringResource(
                        if (isParticipant) R.string.ongoing_session_no_holes_message_participant
                        else R.string.ongoing_session_no_holes_message
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ongoingSession?.let { session ->
                Log.d("OngoingSessionScreen", "session = $session")
                
                // Bandeau de bienvenue: miniatures des joueurs + message localisé
                run {
                    val participantTeam = teamsForSession.find { it.team.id == effectiveTeamId }
                    val p1 = participantTeam?.player1
                    val p2 = participantTeam?.player2
                    val name1 = p1?.name?.ifBlank { null }
                    val name2 = p2?.name?.ifBlank { null }
                    val welcome = when {
                        name1 != null && name2 != null -> stringResource(R.string.ongoing_session_welcome_two_names, name1, name2)
                        name1 != null -> stringResource(R.string.ongoing_session_welcome_one_name, name1)
                        name2 != null -> stringResource(R.string.ongoing_session_welcome_one_name, name2)
                        else -> stringResource(R.string.ongoing_session_welcome_generic)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatars (affichés seulement si des joueurs existent)
                        if (p1 != null || p2 != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (p1?.photoUri?.isNotBlank() == true) {
                                    RemoteImage(
                                        url = p1.photoUri!!,
                                        contentDescription = "Photo ${'$'}{p1.name}",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                } else if (p1 != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                                if (p2?.photoUri?.isNotBlank() == true) {
                                    RemoteImage(
                                        url = p2.photoUri!!,
                                        contentDescription = "Photo ${'$'}{p2.name}",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                } else if (p2 != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }
                        }
                        Text(
                            text = welcome,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Encart: date (gauche)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = session.dateTime.format(
                                    DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Encart: mode de scoring (centre)
                    Card(
                        onClick = { showScoringModeInfo = true },
                        enabled = currentScoringMode != null,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            currentScoringMode?.let { scoringMode ->
                                Text(
                                    text = scoringMode.getLocalizedName(LocalContext.current),
                                    style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Encart: QR (droite)
                    if (!isParticipant) {
                        Card(
                            onClick = { navController.navigate("session_qr") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Afficher le QR de la session",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

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

                    // Carte unique cliquable pour afficher/masquer
                    val topTeam = teamStandings.minWithOrNull(
                        compareBy({ it.totalScore }, { it.totalStrokes })
                    ) ?: teamStandings.first()

                    Card(
                        onClick = { standingsExpanded = !standingsExpanded },
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (standingsExpanded) {
                                // En-tête: libellé + chevron vers le bas
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.standings_table_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Contenu de la table sans sa propre carte et sans titre
                                StandingsTable(
                                    standings = teamStandings,
                                    wrapInCard = false,
                                    showTitle = false
                                )
                            } else {
                                // Vue repliée: libellé + équipe leader sur la même ligne + chevron droit
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.standings_table_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Bloc leader
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.standings_table_title_lead),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = topTeam.teamName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            text = topTeam.totalScore.toString(),
                                            style = MaterialTheme.typography.bodyMedium,

                                            textAlign = androidx.compose.ui.text.style.TextAlign.Right
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isParticipant) {
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
                        text = stringResource(
                            if (isParticipant) R.string.ongoing_session_no_holes_message_participant
                            else R.string.ongoing_session_no_holes_message
                        ),
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
                                    Column {
                                        teamsForSession.forEach { teamWithPlayers ->
                                            val name = listOfNotNull(teamWithPlayers.player1?.name, teamWithPlayers.player2?.name).joinToString(" & ")
                                            val displayName = name.ifBlank { "Equipe ${teamWithPlayers.team.id}" }
                                            val result = playedHole.teamResults.find { it.teamName == name }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = displayName,
                                                    color = if (result == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = ": ",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = if (result == null) "-" else "${result.strokes} - ${result.calculatedScore}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
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
                                                    onPlayedHoleCreated = { _ ->
                                                        // Do not auto-navigate to score entry; stay on current screen
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