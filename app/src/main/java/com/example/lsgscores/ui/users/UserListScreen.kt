package com.example.lsgscores.ui.users

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lsgscores.data.User
import java.io.File
import com.example.lsgscores.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val users by userViewModel.users.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Liste des utilisateurs") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_user") }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(users) { user ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Photo miniature si elle existe, sinon une icône par défaut
                    if (!user.photoUri.isNullOrBlank() && File(user.photoUri).exists()) {
                        val bitmap = remember(user.photoUri) {
                            BitmapFactory.decodeFile(user.photoUri)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "User photo",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_person_24),
                            contentDescription = "Default user icon",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(text = user.name, style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
        }
    }
}
