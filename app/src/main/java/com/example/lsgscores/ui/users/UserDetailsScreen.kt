// ui/users/UserDetailScreen.kt

package com.example.lsgscores.ui.users

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
            // Show loading or not found state
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Player not found")
            }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display large user photo or fallback icon
                val photoExists = !user!!.photoUri.isNullOrBlank() && File(user!!.photoUri!!).exists()
                if (photoExists) {
                    val bitmap = remember(user!!.photoUri) {
                        BitmapFactory.decodeFile(user!!.photoUri)
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
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // User name
                Text(
                    text = user!!.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(32.dp))

                // Row for action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { /* Edit feature not implemented yet */ },
                        enabled = false // Not implemented yet
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

    // Confirmation dialog before deleting the user
    if (showDeleteDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete player") },
            text = { Text("Are you sure you want to delete this player?") },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.deleteUser(user!!)
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

// Don't forget to import your User data class at the top, e.g.
// import com.example.lsgscores.data.user.User
