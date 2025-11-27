package fr.centuryspine.lsgscores.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.centuryspine.lsgscores.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.storage
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class SupabaseStorageHelper @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    private val playersBucketName: String = BuildConfig.SUPABASE_BUCKET_PLAYERS
    private val holesBucketName: String = BuildConfig.SUPABASE_BUCKET_HOLES
    private val sessionsBucketName: String = BuildConfig.SUPABASE_BUCKET_SESSIONS

    suspend fun uploadPlayerPhoto(uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val path = "player_${UUID.randomUUID()}.$ext"
        return uploadToBucket(uri, playersBucketName, path)
    }

    /**
     * Mark a given session photo (provided as its public URL) as the favorite photo for the session.
     * This is implemented by ensuring that exactly one object under "<sessionId>/" has a filename
     * containing the tag "fav_" (e.g. "<sessionId>/fav_photo_xxx.jpg").
     * - If a previous favorite exists, it is renamed to a non-favorite name.
     * - If the provided photo is already favorite, this call is a no-op and succeeds.
     * Returns the public URL of the resulting favorite object on success, or null on failure.
     */
    suspend fun markSessionPhotoAsFavorite(sessionId: Long, publicUrl: String): String? {
        return try {
            val parsed = parseBucketAndObjectFromPublicUrl(publicUrl) ?: return null
            val (bucketInUrl, objectPath) = parsed
            // Ensure we operate on the sessions bucket
            if (!bucketInUrl.equals(sessionsBucketName, ignoreCase = true)) return null

            val bucketRef = client.storage.from(bucketInUrl)

            val fileName = objectPath.substringAfterLast('/')
            objectPath.substringBeforeLast('/', missingDelimiterValue = "")
            val sessionPrefix = sessionId.toString()
            // Defensive: ensure the provided path matches the requested session prefix
            if (!objectPath.startsWith("$sessionPrefix/")) return null

            // If already marked as favorite, return as-is
            if (fileName.startsWith("fav_")) {
                return bucketRef.publicUrl(objectPath)
            }

            // Locate existing favorite in the same session directory
            val items = try {
                bucketRef.list(sessionPrefix)
            } catch (t: Throwable) {
                emptyList()
            }
            val currentFavRelName = items.firstOrNull { (it.name ?: "").startsWith("fav_") }?.name
            // If there is an existing favorite and it's not the target, rename it to a non-favorite name
            if (!currentFavRelName.isNullOrBlank() && currentFavRelName != fileName) {
                val ext = getFileExtension(currentFavRelName) ?: "jpg"
                val newNonFavName = "photo_${UUID.randomUUID()}.$ext"
                val fromPath = "$sessionPrefix/$currentFavRelName"
                val toPath = "$sessionPrefix/$newNonFavName"
                safeMoveObject(bucketRef, fromPath, toPath)
            }

            // Rename the target to a favorite name
            val targetExt = getFileExtension(fileName) ?: "jpg"
            val newFavPath = "$sessionPrefix/fav_photo_${UUID.randomUUID()}.$targetExt"
            safeMoveObject(bucketRef, objectPath, newFavPath)
            bucketRef.publicUrl(newFavPath)
        } catch (t: Throwable) {
            Log.w("Storage", "Failed to mark favorite for $publicUrl", t)
            null
        }
    }

    suspend fun uploadHolePhoto(type: PhotoType, uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val segment = when (type) {
            PhotoType.START -> "start"; PhotoType.END -> "end"
        }
        val path = "hole_${segment}_${UUID.randomUUID()}.$ext"
        return uploadToBucket(uri, holesBucketName, path)
    }

    /**
     * Upload a session photo into the Sessions bucket.
     * The object path always contains the session id for organization: "Session/<id>/<uuid>.<ext>".
     * Returns the public URL of the uploaded object (signed at render time if bucket is private).
     */
    suspend fun uploadSessionPhoto(sessionId: Long, uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val path = "${sessionId}/photo_${UUID.randomUUID()}.${ext}"
        return uploadToBucket(uri, sessionsBucketName, path)
    }

    /**
     * List all photo object URLs for a given session id from the Sessions bucket.
     * Objects are expected to be stored under the "<sessionId>/" prefix.
     * Returns public (or authenticated) URLs; UI will sign them at render time if required.
     */
    suspend fun listSessionPhotos(sessionId: Long): List<String> {
        return listAllObjectsInBucket(sessionsBucketName, sessionId.toString())
    }

    /**
     * Delete a single session photo by its public URL from the Sessions bucket.
     * Returns true if the deletion succeeds, false otherwise.
     */
    suspend fun deleteSessionPhotoByUrl(publicUrl: String): Boolean {
        return deleteByPublicUrl(publicUrl, sessionsBucketName)
    }

    suspend fun deletePlayerPhotoByUrl(publicUrl: String): Boolean {
        return deleteByPublicUrl(publicUrl, playersBucketName)
    }

    suspend fun deleteHolePhotoByUrl(publicUrl: String): Boolean {
        return deleteByPublicUrl(publicUrl, holesBucketName)
    }

    private suspend fun deleteByPublicUrl(publicUrl: String, expectedBucket: String): Boolean {
        // Supabase URL format: <base>/storage/v1/object/<visibility>/<bucket>/<path>[?query]
        // Handle both public and authenticated URLs; be robust to query params and case differences.
        return try {
            val marker = "/storage/v1/object/"
            val start = publicUrl.indexOf(marker)
            if (start == -1) return false
            val after = publicUrl.substring(start + marker.length) // "<visibility>/<bucket>/<object>[?...]"
            val firstSlash = after.indexOf('/')
            if (firstSlash <= 0) return false
            val afterVisibility = after.substring(firstSlash + 1) // "<bucket>/<object>[?...]"
            val secondSlash = afterVisibility.indexOf('/')
            if (secondSlash <= 0) return false
            val bucketInUrl = afterVisibility.take(secondSlash)
            var objectPath = afterVisibility.substring(secondSlash + 1)
            val q = objectPath.indexOf('?')
            if (q != -1) objectPath = objectPath.take(q)
            objectPath = Uri.decode(objectPath)
            if (!bucketInUrl.equals(expectedBucket, ignoreCase = true) || objectPath.isBlank()) return false
            val bucketRef = client.storage.from(bucketInUrl) // use exact bucket from URL
            // Try to delete a single path; if unsupported, this will throw, and we return false
            bucketRef.delete(objectPath)
            true
        } catch (t: Throwable) {
            Log.w("Storage", "Delete failed for $publicUrl", t)
            false
        }
    }

    private suspend fun uploadToBucket(uri: Uri, bucket: String, path: String): String {
        val bytes = readBytesFromUri(uri)
            ?: throw IllegalArgumentException("Unable to read image data from URI or file: $uri")

        val bucketRef = client.storage.from(bucket)
        // Upsert to avoid failures if we retry
        bucketRef.upload(path, bytes, upsert = true)
        // Return a "public" URL; if the bucket is private, the UI will generate a signed URL at render time.
        return bucketRef.publicUrl(path)
    }

    /**
     * Fetches all object public URLs under a given prefix in a bucket, using pagination.
     */
    private suspend fun listAllObjectsInBucket(bucket: String, prefix: String): List<String> {
        val bucketRef = client.storage.from(bucket)
        val normalizedPrefix = prefix.trim('/', '\\')
        return try {
            // Use the first positional parameter for compatibility across supabase-kt versions
            val items = bucketRef.list(normalizedPrefix)
            items.mapNotNull { item ->
                val name = item.name
                if (name.isNullOrBlank()) null else {
                    val objectPath = if (normalizedPrefix.isBlank()) name else "$normalizedPrefix/$name"
                    bucketRef.publicUrl(objectPath)
                }
            }
        } catch (t: Throwable) {
            Log.w("Storage", "Failed to list objects in $bucket at prefix $normalizedPrefix", t)
            emptyList()
        }
    }

    suspend fun getSignedUrlForPublicUrl(url: String, expiresInSeconds: Int = 604800): String? {
        // Accepts URLs like:
        // - .../storage/v1/object/public/<bucket>/<path>
        // - .../storage/v1/object/authenticated/<bucket>/<path>
        // If URL is already signed (contains "/object/sign/" or token param), returns it as-is.
        return try {
            val uri = url.toUri()
            if ((uri.path ?: "").contains("/storage/v1/object/sign/") || (uri.query ?: "").contains("token=")) {
                return url
            }
            val path = uri.path ?: return null
            val marker = "/storage/v1/object/"
            val idx = path.indexOf(marker)
            if (idx == -1) return null
            val after = path.substring(idx + marker.length) // e.g., "public/<bucket>/<object>"
            val firstSlash = after.indexOf('/')
            if (firstSlash <= 0) return null
            // visibility segment (public/authenticated) is not needed for signing
            val afterVisibility = after.substring(firstSlash + 1) // "<bucket>/<object>"
            val secondSlash = afterVisibility.indexOf('/')
            if (secondSlash <= 0) return null
            val bucket = afterVisibility.take(secondSlash)
            val objectPath = afterVisibility.substring(secondSlash + 1)
            if (objectPath.isBlank()) return null
            val bucketRef = client.storage.from(bucket)
            bucketRef.createSignedUrl(objectPath, expiresInSeconds.seconds)
        } catch (_: Throwable) {
            null
        }
    }

    // --- Helpers for public URL parsing and object operations ---
    private fun parseBucketAndObjectFromPublicUrl(publicUrl: String): Pair<String, String>? {
        return try {
            val marker = "/storage/v1/object/"
            val start = publicUrl.indexOf(marker)
            if (start == -1) return null
            val after = publicUrl.substring(start + marker.length)
            val firstSlash = after.indexOf('/')
            if (firstSlash <= 0) return null
            val afterVisibility = after.substring(firstSlash + 1)
            val secondSlash = afterVisibility.indexOf('/')
            if (secondSlash <= 0) return null
            val bucketInUrl = afterVisibility.take(secondSlash)
            var objectPath = afterVisibility.substring(secondSlash + 1)
            val q = objectPath.indexOf('?')
            if (q != -1) objectPath = objectPath.take(q)
            objectPath = Uri.decode(objectPath)
            if (objectPath.isBlank()) return null
            bucketInUrl to objectPath
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun safeMoveObject(
        bucketRef: BucketApi,
        fromPath: String,
        toPath: String
    ) {
        try {
            // Try native move first (if supported by the library version)
            bucketRef.move(fromPath, toPath)
        } catch (t: Throwable) {
            // Fallback: read via public URL (signed if needed) and upload to new path, then delete old
            try {
                val basePublic = bucketRef.publicUrl(fromPath)
                val signed = getSignedUrlForPublicUrl(basePublic) ?: basePublic
                val bytes = java.net.URL(signed).openStream().use { it.readBytes() }
                bucketRef.upload(toPath, bytes, upsert = true)
                // Attempt delete of the old path regardless of upload success
                try {
                    bucketRef.delete(fromPath)
                } catch (_: Throwable) {
                }
            } catch (tt: Throwable) {
                throw tt
            }
        }
    }

    private fun getFileExtension(fileName: String): String? {
        val dot = fileName.lastIndexOf('.')
        return if (dot != -1 && dot < fileName.length - 1) fileName.substring(dot + 1) else null
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            when (uri.scheme?.lowercase()) {
                "content" -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                "file" -> uri.path?.let { p -> File(p).takeIf { it.exists() }?.readBytes() }
                "http", "https" -> java.net.URL(uri.toString()).openStream().use { it.readBytes() }
                null -> uri.path?.let { p -> File(p).takeIf { it.exists() }?.readBytes() }
                else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun detectExtension(contentResolver: ContentResolver, uri: Uri): String? {
        val mime = contentResolver.getType(uri)
        if (mime != null) {
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        }
        val last = uri.lastPathSegment ?: return null
        val dot = last.lastIndexOf('.')
        return if (dot != -1 && dot < last.length - 1) last.substring(dot + 1) else null
    }

    enum class PhotoType { START, END }
}
