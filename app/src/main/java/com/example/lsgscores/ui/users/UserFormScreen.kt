package com.example.lsgscores.ui.users

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.lsgscores.R
import com.example.lsgscores.ui.common.PhotoPicker
import com.example.lsgscores.viewmodel.UserViewModel
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var name by remember { mutableStateOf("") }

    // State for the cropped photo URI
    var croppedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoPath by remember { mutableStateOf<String?>(null) }

    // Camera permission state
    val context = LocalContext.current

    // Launcher for cropping image
    val cropImageLauncher = rememberLauncherForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                croppedPhotoUri = croppedUri
                val savedPath = saveCroppedImageToInternalStorage(context, croppedUri)
                photoPath = savedPath
            }
        }

    }

    // Launcher for picking from gallery
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cropImageLauncher.launch(
                CropImageContractOptions(it, CropImageOptions())
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add a street golf player") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically){
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.weight(1f)
            )

            // Button to take photo
                PhotoPicker(
                    label = "Take target picture",
                    iconResId = R.drawable.baseline_camera_alt_24,
                    onPhotoPicked = { path -> photoPath = path }
                )


            // Button to pick photo from gallery
            IconButton(                                onClick = {
                pickImageLauncher.launch("image/*")
            }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_add_photo_alternate_24),
                    contentDescription = "Pick from gallery"
                )
            }
            }
            Spacer(Modifier.height(16.dp))

            // Show cropped photo if available
            croppedPhotoUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "User photo",
                    modifier = Modifier.size(128.dp)
                )
            }


            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        userViewModel.addUser(
                            name = name,
                            photoUri = photoPath
                        ) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
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
        val fileName = "user_photo_${UUID.randomUUID()}.jpg"
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