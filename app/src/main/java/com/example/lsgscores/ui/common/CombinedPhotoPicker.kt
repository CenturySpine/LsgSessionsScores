package com.example.lsgscores.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CombinedPhotoPicker(
    modifier: Modifier = Modifier,
    onImagePicked: (String?) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhotoPicker(
            modifier,
            onPhotoPicked = onImagePicked
        )
        Spacer(modifier = Modifier.width(8.dp))
        GalleryPhotoPicker(
            onPhotoPicked = onImagePicked
        )
    }
}
