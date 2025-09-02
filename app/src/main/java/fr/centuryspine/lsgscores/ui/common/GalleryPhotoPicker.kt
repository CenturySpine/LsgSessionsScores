package fr.centuryspine.lsgscores.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import fr.centuryspine.lsgscores.R

/**
 * Provides gallery launcher functionality without UI.
 * Returns a function that can be called to trigger the gallery picker.
 */
@Composable
fun usePhotoGalleryLauncher(
    onPhotoPicked: (String?) -> Unit
): () -> Unit {
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

    return {
        pickImageLauncher.launch("image/*")
    }
}

@Composable
fun GalleryPhotoPicker(
    modifier: Modifier = Modifier,
    onPhotoPicked: (String?) -> Unit
) {
    val launchGallery = usePhotoGalleryLauncher(onPhotoPicked)

    IconButton(
        onClick = launchGallery,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_add_photo_alternate_24),
            contentDescription = stringResource(R.string.photo_picker_gallery_description)
        )
    }
}