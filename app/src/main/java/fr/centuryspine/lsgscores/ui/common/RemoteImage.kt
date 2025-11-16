package fr.centuryspine.lsgscores.ui.common

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fr.centuryspine.lsgscores.utils.ImageCacheKeyHelper
import fr.centuryspine.lsgscores.utils.SupabaseStorageHelper

/**
 * Remote image composable that prefers local cache.
 *
 * Key change: we set a stable custom cache key derived from the Supabase bucket/object path
 * so that prefetch (warm) requests and UI requests map to the same cache entries even if
 * the signed URL tokens differ. For non-Supabase URLs we fall back to the URL itself.
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
    val cacheKey = remember(url) { ImageCacheKeyHelper.stableCacheKey(url) }

    // If it's a Supabase Storage URL and not yet signed, generate a signed URL
    LaunchedEffect(url) {
        val uri = runCatching { url.toUri() }.getOrNull()
        val path = uri?.path.orEmpty()
        val isSupabaseStorage = path.contains("/storage/v1/object/")
        val isAlreadySigned = path.contains("/storage/v1/object/sign/") || (uri?.query.orEmpty().contains("token="))
        if (isSupabaseStorage && !isAlreadySigned) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RemoteImageEntryPoint::class.java
            )
            val signed = entryPoint.supabaseStorageHelper().getSignedUrlForPublicUrl(url)
            targetUrl = if (!signed.isNullOrBlank()) signed else url
        } else {
            targetUrl = url
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(targetUrl)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
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
