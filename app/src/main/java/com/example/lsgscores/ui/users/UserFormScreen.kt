package com.example.lsgscores.ui.users

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var name by remember { mutableStateOf("") }
    var shouldLaunchCamera by remember { mutableStateOf(false) }

    // État pour la photo prise
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // State to store if permission is granted
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher to request CAMERA permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Launcher pour prendre une photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBitmap = bitmap
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        val filename = "user_photo_${System.currentTimeMillis()}.jpg"
        return try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            // Retourne le chemin absolu du fichier
            File(context.filesDir, filename).absolutePath
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(hasCameraPermission, shouldLaunchCamera) {
        if (hasCameraPermission && shouldLaunchCamera) {
            shouldLaunchCamera = false
            cameraLauncher.launch(null)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajouter un utilisateur") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            // Affichage de la photo si prise
            photoBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Photo du joueur",
                    modifier = Modifier.size(128.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            // Bouton pour prendre une photo
            Button(onClick = {
                if (hasCameraPermission) {
                    cameraLauncher.launch(null)
                } else {
                    shouldLaunchCamera = true // On veut prendre la photo dès que la permission est donnée
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("Prendre une photo")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()  ) {
                        val uri = photoBitmap?.let { saveBitmapToInternalStorage(context, it) }
                        userViewModel.addUser(name,uri) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer")
            }
        }
    }
}
