// ui/users/UserDetailScreen.kt

package com.example.lsgscores.ui.users

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.R
import com.example.lsgscores.viewmodel.UserViewModel
import com.example.lsgscores.data.user.User
import com.example.lsgscores.ui.common.CombinedPhotoPicker
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    navController: NavController,
    userId: Long,
    userViewModel: UserViewModel
) {
    val users by userViewModel.users.collectAsState(initial = emptyList())
    val user = users.find { it.id == userId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State for editing
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhotoPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_close_24),
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (user == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Player not found")
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
                        val photoExists = !photoToShow.isNullOrBlank() && File(photoToShow).exists()
                        if (photoExists) {
                            val bitmap = remember(photoToShow) {
                                BitmapFactory.decodeFile(photoToShow)
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "User photo",
                                    modifier = Modifier
                                        .size(400.dp)
                                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_person_24),
                                contentDescription = "Default user icon",
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
                            label = { Text("Name") },
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
                                    userViewModel.updateUser(
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
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                editedName = user.name
                                editedPhotoPath = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
            // READ-ONLY MODE
            else {
                Column(
                    Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val photoExists = !user.photoUri.isNullOrBlank() && File(user.photoUri!!).exists()
                    if (photoExists) {
                        val bitmap = remember(user.photoUri) {
                            BitmapFactory.decodeFile(user.photoUri)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "User photo",
                                modifier = Modifier
                                    .size(400.dp)
                                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_person_24),
                            contentDescription = "Default user icon",
                            modifier = Modifier.size(400.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(32.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                // Activate edit mode, initialize values
                                editedName = user.name
                                editedPhotoPath = null
                                isEditing = true
                            }
                        ) {
                            Text("Edit")
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
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
            title = { Text("Delete player") },
            text = { Text("Are you sure you want to delete this player?") },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.deleteUser(user)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
