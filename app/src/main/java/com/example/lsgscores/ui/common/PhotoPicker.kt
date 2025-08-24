package com.example.lsgscores.ui.common

import android.Manifest
import android.content.Context
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.lsgscores.R
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoPicker(
    modifier: Modifier = Modifier,
    onPhotoPicked: (String?) -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Hold the latest captured photo URI and its File
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    // Cropper launcher (unchanged)
    val cropImageLauncher = rememberLauncherForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                val savedPath = saveCroppedImageToInternalStorage(context, croppedUri)
                onPhotoPicked(savedPath)
            }
        }
    }

    // High-res photo launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null && photoFile != null) {
            // Save the original (uncropped) photo to the gallery
            savePhotoToGallery(context, photoFile!!)
            // Continue with cropping for app use
            cropImageLauncher.launch(
                CropImageContractOptions(photoUri, CropImageOptions())
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            launchCamera(context) { uri, file ->
                photoUri = uri
                photoFile = file
                takePictureLauncher.launch(uri)
            }
        }
    }

    IconButton(
        onClick = {
            if (hasCameraPermission) {
                launchCamera(context) { uri, file ->
                    photoUri = uri
                    photoFile = file
                    takePictureLauncher.launch(uri)
                }
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
            contentDescription = stringResource(R.string.photo_picker_camera_description)
        )
    }
}

/**
 * Helper to launch the camera with a temp file and pass both Uri and File.
 */
private fun launchCamera(context: Context, onReady: (Uri, File) -> Unit) {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "photo_${timeStamp}.jpg"
    val photoFile = File(context.filesDir, imageFileName)

    val photoUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    onReady(photoUri, photoFile)
}

/**
 * Save the cropped image into app storage, return absolute path.
 */
fun saveCroppedImageToInternalStorage(context: Context, sourceUri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        val fileName = "user_photo_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream: OutputStream = file.outputStream()
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Save the original photo file into the public gallery (MediaStore) in the "StreetGolf" album.
 * Returns the URI of the newly inserted photo, or null if it fails.
 */
fun savePhotoToGallery(context: Context, photoFile: File): Uri? {
    val contentResolver = context.contentResolver
    val fileName = photoFile.name
    val mimeType = "image/jpeg"

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StreetGolf")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri != null) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(photoFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}