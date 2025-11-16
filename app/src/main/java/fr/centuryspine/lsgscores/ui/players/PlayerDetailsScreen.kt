// ui/users/PlayerDetailScreen.kt

package fr.centuryspine.lsgscores.ui.players

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
    users.find { it.id == userId }

    // State for editing
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editedName by rememberSaveable { mutableStateOf("") }
    var editedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Remember the last swipe direction (-1 = left/next, 1 = right/prev, 0 = none)
    var lastSwipeDirection by remember { mutableIntStateOf(0) }

    // For swipe navigation
    // Sort users by id (or whatever ordering you want)
    val sortedUsers = remember(users) { users.sortedBy { it.name } }
    val currentIndex = sortedUsers.indexOfFirst { it.id == userId }

    // Detect swipe gesture only in read-only mode
    val swipeModifier = if (!isEditing && sortedUsers.size > 1 && currentIndex != -1) {
        Modifier.pointerInput(currentIndex, sortedUsers.size, isEditing) {
            var totalDrag = 0f
            var didNavigate = false
            detectHorizontalDragGestures(
                onDragStart = {
                    totalDrag = 0f
                    didNavigate = false
                },
                onHorizontalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    if (!didNavigate) {
                        if (totalDrag > 150f) { // Swipe right (previous)
                            val prevIndex = if (currentIndex == 0) sortedUsers.lastIndex else currentIndex - 1
                            // Moving to previous: new content should enter from left
                            lastSwipeDirection = 1
                            didNavigate = true
                            navController.navigate("user_detail/${sortedUsers[prevIndex].id}") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        } else if (totalDrag < -150f) { // Swipe left (next)
                            val nextIndex = if (currentIndex == sortedUsers.lastIndex) 0 else currentIndex + 1
                            // Moving to next: new content should enter from right
                            lastSwipeDirection = -1
                            didNavigate = true
                            navController.navigate("user_detail/${sortedUsers[nextIndex].id}") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Scaffold { padding ->
        AnimatedContent(
            targetState = userId,
            transitionSpec = {
                val duration = 260
                when {
                    lastSwipeDirection < 0 -> {
                        (slideInHorizontally(animationSpec = tween(duration)) { it } +
                                fadeIn(animationSpec = tween(duration))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(duration)) { -it } +
                                        fadeOut(animationSpec = tween(duration)))
                    }

                    lastSwipeDirection > 0 -> {
                        (slideInHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeIn(animationSpec = tween(duration))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(duration)) { it } +
                                        fadeOut(animationSpec = tween(duration)))
                    }

                    else -> {
                        fadeIn(animationSpec = tween(duration)) togetherWith fadeOut(animationSpec = tween(duration))
                    }
                }
            },
            label = "PlayerDetailTransition"
        ) { targetUserId ->
            val user = users.find { it.id == targetUserId }

            // Only allow edit if the displayed player belongs to the currently authenticated app user
            val canEdit =
                user != null &&
                        remember(user.id) { playerViewModel.isCurrentAuthenticatedUser(user) }

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
                        // Show an edited photo or current photo
                        val photoToShow = editedPhotoPath ?: user?.photoUri
                        if (!photoToShow.isNullOrBlank()) {
                            fr.centuryspine.lsgscores.ui.common.RemoteImage(
                                url = photoToShow,
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

                    // Sticky Save/Cancel Row (centered)
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                if (user == null) return@Button
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
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = {
                                if (user == null) return@OutlinedButton
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
                ) {
                    // Player name above the photo
                    Text(
                        text = user?.name ?: "unknown player",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(32.dp))

                    // Photo below the name
                    if (!user?.photoUri.isNullOrBlank()) {
                        fr.centuryspine.lsgscores.ui.common.RemoteImage(
                            url = user.photoUri,
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

                    // Action buttons row
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                        OutlinedButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text(stringResource(R.string.player_detail_button_back))
                        }
                        if (canEdit && user != null) {
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
                        }
                    }

                }
            }
        }

    }


}
