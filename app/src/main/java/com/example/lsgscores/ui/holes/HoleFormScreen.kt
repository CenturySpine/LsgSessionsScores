package com.example.lsgscores.ui.holes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lsgscores.viewmodel.HoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleFormScreen(navController: NavHostController, holeViewModel: HoleViewModel) {

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add a hole") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {}
    }
}