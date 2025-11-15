package fr.centuryspine.lsgscores.ui.players

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.viewmodel.PlayerViewModel

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

    // Local list that mirrors the Flow and can be updated optimistically after deletions
    val localPlayers = remember { mutableStateListOf<Player>() }

    // Keep local list in sync with source whenever source changes (e.g., city switch)
    androidx.compose.runtime.LaunchedEffect(users) {
        localPlayers.clear()
        localPlayers.addAll(users.sortedBy { it.name })
    }

    // Handle return from detail screen: remove deleted player id if provided
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val deletedIdFlow = remember(savedStateHandle) {
        savedStateHandle?.getStateFlow("deletedPlayerId", -1L)
    }
    val deletedPlayerId = deletedIdFlow?.collectAsStateWithLifecycle(
        lifecycle = LocalLifecycleOwner.current.lifecycle,
        initialValue = -1L
    )?.value ?: -1L

    androidx.compose.runtime.LaunchedEffect(deletedPlayerId) {
        if (deletedPlayerId > 0) {
            localPlayers.removeAll { it.id == deletedPlayerId }
            savedStateHandle?.set("deletedPlayerId", -1L)
        }
    }

    Scaffold(
        topBar = {

        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(localPlayers, key = { it.id }) { player ->
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
                        fr.centuryspine.lsgscores.ui.common.RemoteImage(
                            url = player.photoUri,
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

                }
                HorizontalDivider()
            }
        }
    }
}
