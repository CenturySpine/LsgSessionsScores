package com.example.lsgscores.ui.holes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.lsgscores.R
import com.example.lsgscores.data.Hole
import com.example.lsgscores.data.HolePoint

import com.example.lsgscores.ui.users.saveCroppedImageToInternalStorage
import com.example.lsgscores.viewmodel.HoleViewModel
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleFormScreen(
    navController: NavHostController,
    holeViewModel: HoleViewModel
) {
    val context = LocalContext.current
    var showNameError by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var geoZone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var par by remember { mutableStateOf("") }

    var startName by remember { mutableStateOf("") }
    var endName by remember { mutableStateOf("") }


    var startPhotoPath by remember { mutableStateOf<String?>(null) }

    var endPhotoPath by remember { mutableStateOf<String?>(null) }

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val startCropImageLauncher = rememberLauncherForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->

                val savedPath = saveCroppedImageToInternalStorage(context, croppedUri)
                startPhotoPath = savedPath
            }
        }

    }
    val endCropImageLauncher = rememberLauncherForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                
                val savedPath = saveCroppedImageToInternalStorage(context, croppedUri)
                endPhotoPath = savedPath
            }
        }

    }

    val startTakePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            // Convert Bitmap to Uri for cropping
            val imageUri = saveBitmapToCacheAndGetUri(context, it)
            imageUri?.let { uri ->
                startCropImageLauncher.launch(
                    CropImageContractOptions(uri, CropImageOptions())
                )
            }
        }
    }
    val endTakePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            // Convert Bitmap to Uri for cropping
            val imageUri = saveBitmapToCacheAndGetUri(context, it)
            imageUri?.let { uri ->
                endCropImageLauncher.launch(
                    CropImageContractOptions(uri, CropImageOptions())
                )
            }
        }
    }


    val startCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            // If permission is granted, trigger camera photo selection immediately
            startTakePictureLauncher.launch(null)
        }
    }
    val endCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            // If permission is granted, trigger camera photo selection immediately
            endTakePictureLauncher.launch(null)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add a hole") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (showNameError) {
                Text(
                    text = "Le nom du trou est obligatoire.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hole name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // GeoZone
            OutlinedTextField(
                value = geoZone,
                onValueChange = { geoZone = it },
                label = { Text("Area") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Description (multilignes)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4
            )
            Spacer(Modifier.height(12.dp))

            // Constraints (multilignes)
            OutlinedTextField(
                value = constraints,
                onValueChange = { constraints = it },
                label = { Text("Constraints") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4
            )
            Spacer(Modifier.height(12.dp))

            // Distance (numérique)
            OutlinedTextField(
                value = distance,
                onValueChange = { distance = it.filter { c -> c.isDigit() } },
                label = { Text("Distance (m)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(12.dp))

            // Par (numérique)
            OutlinedTextField(
                value = par,
                onValueChange = { par = it.filter { c -> c.isDigit() } },
                label = { Text("Par") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(24.dp))

            // Start and End Points - Row with 2 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Starting Point (Left)
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text("Start", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = startName,
                        onValueChange = { startName = it },
                        label = { Text("Start name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    IconButton(
                        onClick = {
                            if (hasCameraPermission) {
                                startTakePictureLauncher.launch(null)
                            } else {
                                startCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                            contentDescription = "Take start picture"
                        )
                    }
                }

                // Ending Point (Right)
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text("Target", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endName,
                        onValueChange = { endName = it },
                        label = { Text("Target name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    IconButton(
                        onClick = {
                            if (hasCameraPermission) {
                                endTakePictureLauncher.launch(null)
                            } else {
                                endCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                            contentDescription = "Take target picture"
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))


            Button(
                onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                    } else {
                        showNameError = false
                        val hole = Hole(
                            name = name,
                            geoZone = geoZone,
                            description = description.takeIf { it.isNotBlank() },
                            constraints = constraints.takeIf { it.isNotBlank() },
                            distance = distance.toIntOrNull(),
                            par = par.toIntOrNull()
                                ?: 3, // Valeur par défaut, à ajuster selon tes besoins
                            start = HolePoint(
                                name = startName,
                                photoUri = startPhotoPath

                            ),
                            end = HolePoint(
                                name = endName,
                                photoUri = endPhotoPath

                            )
                        )
                        holeViewModel.addHole(hole)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save")
            }
        }
    }
}

// Helper: Save Bitmap to cache, return content URI (required for CropImage)
private fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri? {
    val filename = "photo_${System.currentTimeMillis()}.png"
    val stream = context.openFileOutput(filename, Context.MODE_PRIVATE)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.close()
    val file = context.getFileStreamPath(filename)
    return try {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveCroppedImageToInternalStorage(context: Context, sourceUri: Uri): String? {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        val fileName = "hole_photo_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream: OutputStream = file.outputStream()
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}