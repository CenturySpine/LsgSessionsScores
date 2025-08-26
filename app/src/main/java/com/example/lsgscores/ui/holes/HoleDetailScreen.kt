// ui/holes/HoleDetailScreen.kt

package com.example.lsgscores.ui.holes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.lsgscores.R
import com.example.lsgscores.viewmodel.HoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleDetailScreen(
    navController: NavController,
    holeId: Long?,
    holeViewModel: HoleViewModel = hiltViewModel()
) {
    val hole by holeViewModel.getHoleById(holeId ?: 0).collectAsState(initial = null)
    var editedName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(hole) {
        hole?.let {
            editedName = it.name
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            hole?.let {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Nom du trou") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(text = it.name, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!it.startPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = it.startPhotoUri,
                            contentDescription = stringResource(R.string.hole_list_photo_description),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.GolfCourse,
                            contentDescription = stringResource(R.string.hole_list_default_start_icon_description),
                            modifier = Modifier
                                .size(128.dp)
                                .weight(1f)
                        )
                    }

                    if (!it.endPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = it.endPhotoUri,
                            contentDescription = stringResource(R.string.hole_list_photo_description),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.GolfCourse,
                            contentDescription = stringResource(R.string.hole_list_default_end_icon_description),
                            modifier = Modifier
                                .size(128.dp)
                                .weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                it.description?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                it.distance?.let {
                    Text(text = "Distance: $it m", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(text = "Par: ${it.par}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { navController.popBackStack() }) {
                        Text(stringResource(R.string.hole_details_back))
                    }
                    if (isEditing) {
                        Row {
                            Button(onClick = {
                                hole?.let {
                                    val updatedHole = it.copy(name = editedName)
                                    holeViewModel.updateHole(updatedHole) {
                                        isEditing = false
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.hole_details_save))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { isEditing = false }) {
                                Text(stringResource(R.string.hole_details_cancel))
                            }
                        }
                    } else {
                        Button(onClick = { isEditing = true }) {
                            Text(stringResource(R.string.hole_details_edit))
                        }
                    }
                }
            } ?: run {
                Text(text = "Aucun trou sélectionné")
            }
        }
    }
}
