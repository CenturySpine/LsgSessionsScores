package com.example.lsgscores.ui.players

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lsgscores.data.player.Player
import java.io.File
import com.example.lsgscores.R
import com.example.lsgscores.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    val users by playerViewModel.players.collectAsStateWithLifecycle(
        lifecycle = LocalLifecycleOwner.current.lifecycle,
        initialValue = emptyList()
    )
    val sortedPlayers = remember(users) { users.sortedBy { it.name } }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showDialog by remember { mutableStateOf(false) }

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

                    if (!player.photoUri.isNullOrBlank() && File(player.photoUri).exists()) {
                        val bitmap = remember(player.photoUri) {
                            BitmapFactory.decodeFile(player.photoUri)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.player_list_photo_description),
                                modifier = Modifier.size(48.dp)
                            )
                        }
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
