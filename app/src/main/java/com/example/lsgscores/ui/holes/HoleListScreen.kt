// ui/holes/HoleListScreen.kt

package com.example.lsgscores.ui.holes

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleListScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Holes") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("No holes to display", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
