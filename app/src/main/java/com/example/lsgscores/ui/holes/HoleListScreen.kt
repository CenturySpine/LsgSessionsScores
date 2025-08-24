// ui/holes/HoleListScreen.kt

package com.example.lsgscores.ui.holes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lsgscores.R
import com.example.lsgscores.data.hole.Hole
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
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_hole") }) {
                Text(stringResource(R.string.hole_list_fab_add))
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

                    if (!holeItem.start.photoUri.isNullOrBlank() && File(holeItem.start.photoUri).exists()) {
                        val bitmap = remember(holeItem.start.photoUri) {
                            BitmapFactory.decodeFile(holeItem.start.photoUri)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.hole_list_photo_description),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.GolfCourse,
                            contentDescription = stringResource(R.string.hole_list_default_start_icon_description),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    if (!holeItem.end.photoUri.isNullOrBlank() && File(holeItem.end.photoUri).exists()) {
                        val bitmap = remember(holeItem.end.photoUri) {
                            BitmapFactory.decodeFile(holeItem.end.photoUri)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.hole_list_photo_description),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.GolfCourse,
                            contentDescription = stringResource(R.string.hole_list_default_end_icon_description),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {
                        holeToDelete = holeItem
                        showDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.hole_list_delete_icon_description),
                            tint = MaterialTheme.colorScheme.error
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
            title = { Text(stringResource(R.string.hole_list_dialog_title)) },
            text = { Text(stringResource(R.string.hole_list_dialog_message, holeToDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    holeViewModel.deleteHole(holeToDelete!!)
                    showDialog = false
                }) { Text(stringResource(R.string.hole_list_dialog_button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.hole_list_dialog_button_cancel)) }
            }
        )
    }

}
