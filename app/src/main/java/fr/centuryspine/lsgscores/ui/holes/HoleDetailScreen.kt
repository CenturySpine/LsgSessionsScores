// ui/holes/HoleDetailScreen.kt

package fr.centuryspine.lsgscores.ui.holes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleDetailScreen(
    navController: NavController,
    holeId: Long?,
    holeViewModel: HoleViewModel = hiltViewModel()
) {
    val hole by holeViewModel.getHoleById(holeId ?: 0).collectAsState(initial = null)

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Edit state
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    var editedDistance by remember { mutableStateOf("") }
    var editedPar by remember { mutableStateOf("") }
    var editedStartPhoto by remember { mutableStateOf<String?>(null) }
    var editedEndPhoto by remember { mutableStateOf<String?>(null) }

    // For swipe navigation between holes (readonly mode only)
    val holes by holeViewModel.holes.collectAsState(initial = emptyList())
    val sortedHoles = remember(holes) { holes.sortedBy { it.name } }
    val currentHoleIndex = remember(sortedHoles, holeId) { sortedHoles.indexOfFirst { it.id == (holeId ?: 0L) } }

    val swipeModifier = if (!isEditing && sortedHoles.size > 1 && currentHoleIndex != -1) {
        Modifier.pointerInput(currentHoleIndex, sortedHoles.size, isEditing) {
            var totalDrag = 0f
            var didNavigate = false
            detectHorizontalDragGestures(
                onDragStart = {
                    totalDrag = 0f
                    didNavigate = false
                },
                onHorizontalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    if (!didNavigate) {
                        if (totalDrag > 150f) {
                            val prevIndex = if (currentHoleIndex == 0) sortedHoles.lastIndex else currentHoleIndex - 1
                            didNavigate = true
                            navController.navigate("hole_detail/${sortedHoles[prevIndex].id}") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        } else if (totalDrag < -150f) {
                            val nextIndex = if (currentHoleIndex == sortedHoles.lastIndex) 0 else currentHoleIndex + 1
                            didNavigate = true
                            navController.navigate("hole_detail/${sortedHoles[nextIndex].id}") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }

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
        hole?.let { currentHole ->
            if (isEditing) {
                // Edit mode with sticky Save/Cancel, like PlayerDetailsScreen
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .padding(bottom = 88.dp) // room for sticky buttons
                    ) {
                        // Name
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text(stringResource(R.string.hole_form_label_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Photos + pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (!editedStartPhoto.isNullOrBlank()) {
                                    fr.centuryspine.lsgscores.ui.common.RemoteImage(
                                        url = editedStartPhoto!!,
                                        contentDescription = stringResource(R.string.hole_list_photo_description),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.GolfCourse,
                                        contentDescription = stringResource(R.string.hole_list_default_start_icon_description),
                                        modifier = Modifier.size(128.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                CombinedPhotoPicker(onImagePicked = { path -> editedStartPhoto = path })
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                if (!editedEndPhoto.isNullOrBlank()) {
                                    fr.centuryspine.lsgscores.ui.common.RemoteImage(
                                        url = editedEndPhoto!!,
                                        contentDescription = stringResource(R.string.hole_list_photo_description),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.GolfCourse,
                                        contentDescription = stringResource(R.string.hole_list_default_end_icon_description),
                                        modifier = Modifier.size(128.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                CombinedPhotoPicker(onImagePicked = { path -> editedEndPhoto = path })
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { editedDescription = it },
                            label = { Text(stringResource(R.string.hole_form_label_description)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Distance
                        OutlinedTextField(
                            value = editedDistance,
                            onValueChange = { editedDistance = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.hole_form_label_distance)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Par
                        OutlinedTextField(
                            value = editedPar,
                            onValueChange = { editedPar = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.hole_form_label_par)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Sticky Save/Cancel row (centered)
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
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
                            },
                            enabled = editedName.isNotBlank()
                        ) {
                            Text(stringResource(R.string.hole_details_save))
                        }
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = {
                                // Revert and exit edit mode
                                editedName = currentHole.name
                                editedDescription = currentHole.description ?: ""
                                editedDistance = currentHole.distance?.toString() ?: ""
                                editedPar = currentHole.par.toString()
                                editedStartPhoto = currentHole.startPhotoUri
                                editedEndPhoto = currentHole.endPhotoUri
                                isEditing = false
                            }
                        ) {
                            Text(stringResource(R.string.hole_details_cancel))
                        }
                    }
                }
            } else {
                // Read-only mode with centered buttons like PlayerDetailsScreen
                Column(
                    Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .then(swipeModifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Name
                    Text(text = currentHole.name, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Photos (no pickers)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!currentHole.startPhotoUri.isNullOrBlank()) {
                                fr.centuryspine.lsgscores.ui.common.RemoteImage(
                                    url = currentHole.startPhotoUri!!,
                                    contentDescription = stringResource(R.string.hole_list_photo_description),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.GolfCourse,
                                    contentDescription = stringResource(R.string.hole_list_default_start_icon_description),
                                    modifier = Modifier.size(128.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (!currentHole.endPhotoUri.isNullOrBlank()) {
                                fr.centuryspine.lsgscores.ui.common.RemoteImage(
                                    url = currentHole.endPhotoUri!!,
                                    contentDescription = stringResource(R.string.hole_list_photo_description),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.GolfCourse,
                                    contentDescription = stringResource(R.string.hole_list_default_end_icon_description),
                                    modifier = Modifier.size(128.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description (always shown)
                    Text(
                        text = "Description: " + (currentHole.description?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.pdf_not_applicable)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Distance (always show value or N/A)
                    Text(
                        text = "Distance: " + (currentHole.distance?.let { "$it m" }
                            ?: stringResource(R.string.pdf_not_applicable)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Par
                    Text(text = "Par: ${currentHole.par}", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons row centered: Back + Edit + Delete
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { navController.popBackStack() }) {
                            Text(stringResource(R.string.hole_details_back))
                        }
                        Button(onClick = {
                            // Initialize edited values and enter edit mode
                            editedName = currentHole.name
                            editedDescription = currentHole.description ?: ""
                            editedDistance = currentHole.distance?.toString() ?: ""
                            editedPar = currentHole.par.toString()
                            editedStartPhoto = currentHole.startPhotoUri
                            editedEndPhoto = currentHole.endPhotoUri
                            isEditing = true
                        }) {
                            Text(stringResource(R.string.hole_details_edit))
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.hole_detail_button_delete))
                        }
                    }
                }
            }
        } ?: run {
            Text(text = "Aucun trou sélectionné")
        }
    }

    // Confirmation dialog before deleting the hole
    if (showDeleteDialog && hole != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.hole_detail_dialog_title)) },
            text = { Text(stringResource(R.string.hole_detail_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    hole?.let {
                        holeViewModel.deleteHole(it)
                    }
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text(stringResource(R.string.hole_detail_dialog_button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.hole_detail_dialog_button_cancel)) }
            }
        )
    }
}
