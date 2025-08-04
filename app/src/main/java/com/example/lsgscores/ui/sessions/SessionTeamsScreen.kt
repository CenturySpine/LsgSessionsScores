// ui/sessions/SessionTeamsScreen.kt

package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.lsgscores.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTeamsScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Teams") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Placeholder UI - will be replaced by your chips selector and validation later
        Surface(modifier = Modifier.padding(innerPadding)) {
            Text("Here you will select players and compose the teams for this session.")
        }
    }
}
