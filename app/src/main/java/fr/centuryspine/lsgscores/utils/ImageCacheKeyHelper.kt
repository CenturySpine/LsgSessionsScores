package fr.centuryspine.lsgscores.utils

import android.net.Uri
import androidx.core.net.toUri

/**
 * Helper for generating stable cache keys for Supabase storage URLs.
 * This ensures that different signed URLs pointing to the same object
 * map to the same cache entry.
 */
object ImageCacheKeyHelper {

    /**
     * Generates a stable cache key from a Supabase storage URL.
     * For Supabase URLs, extracts bucket and object path to create a consistent key
     * regardless of URL signing tokens. For non-Supabase URLs, returns the URL itself.
     */
    fun stableCacheKey(inputUrl: String): String {
        return try {
            val uri = inputUrl.toUri()
            val path = uri.path ?: return inputUrl
            val marker = "/storage/v1/object/"
            val idx = path.indexOf(marker)
            if (idx == -1) return inputUrl
            val after = path.substring(idx + marker.length)
            val parts = after.trimStart('/').split('/').filter { it.isNotEmpty() }
            if (parts.size < 2) return inputUrl
            val bucket: String
            val objectPath: String
            if (parts[0] == "sign") {
                if (parts.size < 3) return inputUrl
                bucket = parts[1]
                objectPath = parts.drop(2).joinToString("/")
            } else {
                if (parts.size < 3) return inputUrl
                bucket = parts[1]
                objectPath = parts.drop(2).joinToString("/")
            }
            "supabase:${bucket.lowercase()}/${Uri.decode(objectPath)}"
        } catch (_: Throwable) {
            inputUrl
        }
    }
}
