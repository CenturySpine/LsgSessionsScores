package com.example.lsgscores.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajouter un utilisateur") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Surnom") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && nickname.isNotBlank()) {
                        userViewModel.addUser(name, nickname) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer")
            }
        }
    }
}
