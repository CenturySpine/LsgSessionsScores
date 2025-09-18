package fr.centuryspine.lsgscores.ui.common

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Small helper to load remote images robustly (crossfade, placeholders).
 * Also normalizes Supabase public URL bucket names to lowercase to avoid 404 when
 * the stored URL contains a capitalized bucket segment (endpoints are case-sensitive).
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val normalized = normalizeSupabasePublicUrl(url)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(normalized)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(id = android.R.drawable.ic_menu_report_image),
        error = painterResource(id = android.R.drawable.ic_menu_report_image)
    )
}

private fun normalizeSupabasePublicUrl(url: String): String {
    return try {
        val uri = Uri.parse(url)
        val path = uri.path ?: return url
        val marker = "/storage/v1/object/public/"
        val idx = path.indexOf(marker)
        if (idx == -1) return url
        val prefix = path.substring(0, idx + marker.length)
        val after = path.substring(idx + marker.length)
        val firstSlash = after.indexOf('/')
        if (firstSlash <= 0) return url
        val bucket = after.substring(0, firstSlash)
        val objectPath = after.substring(firstSlash + 1)
        val fixedPath = prefix + bucket.lowercase() + "/" + objectPath
        Uri.Builder()
            .scheme(uri.scheme)
            .authority(uri.authority)
            .path(fixedPath)
            .query(uri.query)
            .fragment(uri.fragment)
            .build()
            .toString()
    } catch (_: Throwable) {
        url
    }
}
