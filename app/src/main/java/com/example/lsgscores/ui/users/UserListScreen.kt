package com.example.lsgscores.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lsgscores.data.User

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
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = user.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = user.nickname, style = MaterialTheme.typography.bodyMedium)
                }
                Divider()
            }
        }
    }
}
