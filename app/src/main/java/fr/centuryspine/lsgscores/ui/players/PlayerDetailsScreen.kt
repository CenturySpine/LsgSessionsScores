// ui/users/PlayerDetailScreen.kt

package fr.centuryspine.lsgscores.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.common.CombinedPhotoPicker
import fr.centuryspine.lsgscores.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    navController: NavController,
    userId: Long,
    playerViewModel: PlayerViewModel
) {
    val users by playerViewModel.players.collectAsState(initial = emptyList())
    val user = users.find { it.id == userId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State for editing
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhotoPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // For swipe navigation
    // Sort users by id (or whatever ordering you want)
    val sortedUsers = remember(users) { users.sortedBy { it.name } }
    val currentIndex = sortedUsers.indexOfFirst { it.id == userId }

    // Detect swipe gesture only in read-only mode
    val swipeModifier = if (!isEditing && sortedUsers.size > 1 && currentIndex != -1) {
        Modifier.pointerInput(currentIndex, sortedUsers.size, isEditing) {
            detectHorizontalDragGestures { _, dragAmount ->
                if (dragAmount > 40) { // Swipe right (previous)
                    val prevIndex =
                        if (currentIndex == 0) sortedUsers.lastIndex else currentIndex - 1
                    navController.navigate("user_detail/${sortedUsers[prevIndex].id}") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                    }
                } else if (dragAmount < -40) { // Swipe left (next)
                    val nextIndex =
                        if (currentIndex == sortedUsers.lastIndex) 0 else currentIndex + 1
                    navController.navigate("user_detail/${sortedUsers[nextIndex].id}") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Scaffold { padding ->
        if (user == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.player_detail_not_found))
            }
        } else {
            // EDIT MODE with sticky Save/Cancel
            if (isEditing) {
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // Scrollable content
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 88.dp), // Room for sticky buttons
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Show edited photo or current photo
                        val photoToShow = editedPhotoPath ?: user.photoUri
                        if (!photoToShow.isNullOrBlank()) {
                            AsyncImage(
                                model = photoToShow,
                                contentDescription = stringResource(R.string.player_detail_photo_description),
                                modifier = Modifier
                                    .size(400.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.medium
                                    )
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_person_24),
                                contentDescription = stringResource(R.string.player_detail_default_icon_description),
                                modifier = Modifier.size(400.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Combined photo picker for editing
                        CombinedPhotoPicker(
                            onImagePicked = { newPath -> editedPhotoPath = newPath }
                        )

                        Spacer(Modifier.height(32.dp))

                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text(stringResource(R.string.player_detail_label_name)) },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }

                    // Sticky Save/Cancel Row
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    playerViewModel.updatePlayer(
                                        user.copy(
                                            name = editedName.trim(),
                                            photoUri = editedPhotoPath ?: user.photoUri
                                        )
                                    )
                                    isEditing = false
                                }
                            },
                            enabled = editedName.isNotBlank()
                        ) {
                            Text(stringResource(R.string.player_detail_button_save))
                        }
                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                editedName = user.name
                                editedPhotoPath = null
                            }
                        ) {
                            Text(stringResource(R.string.player_detail_button_cancel))
                        }
                    }
                }
            }
            // READ-ONLY MODE + swipe
            else {
                Column(
                    Modifier
                        .then(swipeModifier)
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 8.dp)  // Moins d'espace vertical
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                )  {
                    // Player name above the photo
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(32.dp))

                    // Photo below the name
                    if (!user.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = user.photoUri,
                            contentDescription = stringResource(R.string.player_detail_photo_description),
                            modifier = Modifier
                                .size(400.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium
                                )
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_person_24),
                            contentDescription = stringResource(R.string.player_detail_default_icon_description),
                            modifier = Modifier.size(400.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Three buttons: Cancel, Edit, Delete
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text(stringResource(R.string.player_detail_button_back))
                        }
                        Button(
                            onClick = {
                                // Activate edit mode, initialize values
                                editedName = user.name
                                editedPhotoPath = null
                                isEditing = true
                            }
                        ) {
                            Text(stringResource(R.string.player_detail_button_edit))
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.player_detail_button_delete))
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting the user
    if (showDeleteDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.player_detail_dialog_title)) },
            text = { Text(stringResource(R.string.player_detail_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    playerViewModel.deletePlayer(user)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text(stringResource(R.string.player_detail_dialog_button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.player_detail_dialog_button_cancel)) }
            }
        )
    }
}
