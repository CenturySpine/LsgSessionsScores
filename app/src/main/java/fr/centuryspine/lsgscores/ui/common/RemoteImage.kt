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
import androidx.core.net.toUri

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
    val cacheKey = remember(url) { stableCacheKey(url) }

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

private fun stableCacheKey(inputUrl: String): String {
    return try {
        val uri = inputUrl.toUri()
        val path = uri.path ?: return inputUrl
        val marker = "/storage/v1/object/"
        val idx = path.indexOf(marker)
        if (idx == -1) return inputUrl
        val after = path.substring(idx + marker.length) // e.g., "public/<bucket>/<object>" or "sign/<bucket>/<object>"
        val parts = after.trimStart('/').split('/').filter { it.isNotEmpty() }
        if (parts.size < 2) return inputUrl
        val bucket: String
        val objectPath: String
        if (parts[0] == "sign") {
            if (parts.size < 3) return inputUrl
            bucket = parts[1]
            objectPath = parts.drop(2).joinToString("/")
        } else {
            // parts[0] = visibility (public/authenticated), then bucket + object
            if (parts.size < 3) return inputUrl
            bucket = parts[1]
            objectPath = parts.drop(2).joinToString("/")
        }
        "supabase:${bucket.lowercase()}/${Uri.decode(objectPath)}"
    } catch (_: Throwable) {
        inputUrl
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteImageEntryPoint {
    fun supabaseStorageHelper(): SupabaseStorageHelper
}
