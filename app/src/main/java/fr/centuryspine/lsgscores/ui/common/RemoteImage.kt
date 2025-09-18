package fr.centuryspine.lsgscores.ui.common

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fr.centuryspine.lsgscores.utils.SupabaseStorageHelper

/**
 * Small helper to load remote images robustly (crossfade, placeholders).
 * If the URL points to Supabase Storage and is not already signed, we generate
 * a signed URL at render time so that private buckets work too.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current

    var targetUrl by remember(url) { mutableStateOf(url) }

    // If it's a Supabase Storage URL and not yet signed, generate a signed URL
    LaunchedEffect(url) {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        val path = uri?.path.orEmpty()
        val isSupabaseStorage = path.contains("/storage/v1/object/")
        val isAlreadySigned = path.contains("/storage/v1/object/sign/") || (uri?.query.orEmpty().contains("token="))
        if (isSupabaseStorage && !isAlreadySigned) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RemoteImageEntryPoint::class.java
            )
            val signed = entryPoint.supabaseStorageHelper().getSignedUrlForPublicUrl(url)
            if (!signed.isNullOrBlank()) {
                targetUrl = signed
            } else {
                targetUrl = url // fallback
            }
        } else {
            targetUrl = url
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(targetUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(id = android.R.drawable.ic_menu_report_image),
        error = painterResource(id = android.R.drawable.ic_menu_report_image)
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteImageEntryPoint {
    fun supabaseStorageHelper(): SupabaseStorageHelper
}
