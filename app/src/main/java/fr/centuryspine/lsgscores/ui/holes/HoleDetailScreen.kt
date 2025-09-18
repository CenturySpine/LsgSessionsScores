// ui/holes/HoleDetailScreen.kt

package fr.centuryspine.lsgscores.ui.holes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.common.CombinedPhotoPicker
import fr.centuryspine.lsgscores.viewmodel.HoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleDetailScreen(
    navController: NavController,
    holeId: Long?,
    holeViewModel: HoleViewModel = hiltViewModel()
) {
    val hole by holeViewModel.getHoleById(holeId ?: 0).collectAsState(initial = null)

    // Edit state
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    var editedDistance by remember { mutableStateOf("") }
    var editedPar by remember { mutableStateOf("") }
    var editedStartPhoto by remember { mutableStateOf<String?>(null) }
    var editedEndPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hole) {
        hole?.let {
            editedName = it.name
            editedDescription = it.description ?: ""
            editedDistance = it.distance?.toString() ?: ""
            editedPar = it.par.toString()
            editedStartPhoto = it.startPhotoUri
            editedEndPhoto = it.endPhotoUri
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
        ) {
            hole?.let { currentHole ->
                // Name
                if (isEditing) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text(stringResource(R.string.hole_form_label_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(text = currentHole.name, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Photos row with pickers in edit mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!editedStartPhoto.isNullOrBlank()) {
                            AsyncImage(
                                model = editedStartPhoto,
                                contentDescription = stringResource(R.string.hole_list_photo_description),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.GolfCourse,
                                contentDescription = stringResource(R.string.hole_list_default_start_icon_description),
                                modifier = Modifier
                                    .size(128.dp)
                            )
                        }
                        if (isEditing) {
                            Spacer(Modifier.height(8.dp))
                            CombinedPhotoPicker(
                                onImagePicked = { path -> editedStartPhoto = path }
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (!editedEndPhoto.isNullOrBlank()) {
                            AsyncImage(
                                model = editedEndPhoto,
                                contentDescription = stringResource(R.string.hole_list_photo_description),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.GolfCourse,
                                contentDescription = stringResource(R.string.hole_list_default_end_icon_description),
                                modifier = Modifier
                                    .size(128.dp)
                            )
                        }
                        if (isEditing) {
                            Spacer(Modifier.height(8.dp))
                            CombinedPhotoPicker(
                                onImagePicked = { path -> editedEndPhoto = path }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                if (isEditing) {
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text(stringResource(R.string.hole_form_label_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                } else {
                    currentHole.description?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Distance
                if (isEditing) {
                    OutlinedTextField(
                        value = editedDistance,
                        onValueChange = { editedDistance = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.hole_form_label_distance)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    currentHole.distance?.let {
                        Text(text = "Distance: $it m", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Par
                if (isEditing) {
                    OutlinedTextField(
                        value = editedPar,
                        onValueChange = { editedPar = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.hole_form_label_par)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    Text(text = "Par: ${currentHole.par}", style = MaterialTheme.typography.bodyLarge)
                }

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
                                val updatedHole = currentHole.copy(
                                    name = editedName.ifBlank { currentHole.name },
                                    description = editedDescription.takeIf { it.isNotBlank() },
                                    distance = editedDistance.toIntOrNull(),
                                    par = editedPar.toIntOrNull() ?: currentHole.par,
                                    startPhotoUri = editedStartPhoto,
                                    endPhotoUri = editedEndPhoto
                                )
                                holeViewModel.updateHole(updatedHole) {
                                    isEditing = false
                                }
                            }) {
                                Text(stringResource(R.string.hole_details_save))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                // Revert changes
                                editedName = currentHole.name
                                editedDescription = currentHole.description ?: ""
                                editedDistance = currentHole.distance?.toString() ?: ""
                                editedPar = currentHole.par.toString()
                                editedStartPhoto = currentHole.startPhotoUri
                                editedEndPhoto = currentHole.endPhotoUri
                                isEditing = false
                            }) {
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
