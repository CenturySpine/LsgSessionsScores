package com.example.lsgscores.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.lsgscores.R
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

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

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val imageUri = saveBitmapToCacheAndGetUri(context, it)
            imageUri?.let { uri ->
                cropImageLauncher.launch(
                    CropImageContractOptions(uri, CropImageOptions())
                )
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            takePictureLauncher.launch(null)
        }
    }

    IconButton(
        onClick = {
            if (hasCameraPermission) {
                takePictureLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
            contentDescription = "Take picture"
        )
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
