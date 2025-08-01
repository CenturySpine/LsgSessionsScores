package com.example.lsgscores.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.lsgscores.R

@Composable
fun GalleryPhotoPicker(
    modifier: Modifier = Modifier,
    onPhotoPicked: (String?) -> Unit
) {
    val context = LocalContext.current

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                val savedPath = saveCroppedImageToInternalStorage(context, croppedUri)
                onPhotoPicked(savedPath)
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cropImageLauncher.launch(
                CropImageContractOptions(it, CropImageOptions())
            )
        }
    }

    IconButton(
        onClick = { pickImageLauncher.launch("image/*") },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_add_photo_alternate_24),
            contentDescription = "Pick from gallery"
        )
    }
}
