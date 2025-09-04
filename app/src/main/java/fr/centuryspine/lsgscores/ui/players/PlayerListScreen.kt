package fr.centuryspine.lsgscores.ui.players

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.viewmodel.CityViewModel
import fr.centuryspine.lsgscores.viewmodel.PlayerViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    cityViewModel: CityViewModel
) {
    val users by playerViewModel.players.collectAsStateWithLifecycle(
        lifecycle = LocalLifecycleOwner.current.lifecycle,
        initialValue = emptyList()
    )
    val sortedPlayers = remember(users) { users.sortedBy { it.name } }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showDialog by remember { mutableStateOf(false) }
        
    val selectedCityId by cityViewModel.selectedCityId.collectAsState()
    
    LaunchedEffect(selectedCityId) {
        playerViewModel.refreshPlayers()
    }

    Scaffold(
        topBar = {

        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_user") }) {
                Text(stringResource(R.string.player_list_fab_add))
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedPlayers) { player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            navController.navigate("user_detail/${player.id}")
                        }
                ) {

                    if (!player.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = player.photoUri,
                            contentDescription = stringResource(R.string.player_list_photo_description),
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_person_24),
                            contentDescription = stringResource(R.string.player_list_default_icon_description),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))

                    Text(text = player.name, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {
                        playerToDelete = player
                        showDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.player_list_delete_icon_description),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
    if (showDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.player_list_dialog_title)) },
            text = { Text(stringResource(R.string.player_list_dialog_message, playerToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    playerViewModel.deletePlayer(playerToDelete!!)
                    showDialog = false
                }) { Text(stringResource(R.string.player_list_dialog_button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.player_list_dialog_button_cancel)) }
            }
        )
    }
}
