// ui/holes/HoleListScreen.kt

package com.example.lsgscores.ui.holes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lsgscores.R
import com.example.lsgscores.data.Hole
import com.example.lsgscores.data.User
import com.example.lsgscores.viewmodel.HoleViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleListScreen(
    navController: NavController,
    holeViewModel: HoleViewModel
) {
    val holes by holeViewModel.holes.collectAsStateWithLifecycle(
        lifecycle = LocalLifecycleOwner.current.lifecycle,
        initialValue = emptyList()
    )
    var holeToDelete by remember { mutableStateOf<Hole?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Holes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_hole") }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(holes) { holeItem ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {

                        Text(text = holeItem.name, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {
                        holeToDelete = holeItem
                        showDialog = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_delete_forever_24),
                            contentDescription = "Delete hole"
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
    if (showDialog && holeToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${holeToDelete!!.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    holeViewModel.deleteHole(holeToDelete!!)
                    showDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

}
