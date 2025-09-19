package fr.centuryspine.lsgscores.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import fr.centuryspine.lsgscores.BuildConfig
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Singleton
class SupabaseStorageHelper @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    private val playersBucketName: String = BuildConfig.SUPABASE_BUCKET_PLAYERS
    private val holesBucketName: String = BuildConfig.SUPABASE_BUCKET_HOLES

    suspend fun uploadPlayerPhoto(playerId: Long, uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val path = "player_${UUID.randomUUID()}.$ext"
        return uploadToBucket(uri, playersBucketName, path)
    }

    suspend fun uploadHolePhoto(holeId: Long, type: PhotoType, uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val segment = when (type) { PhotoType.START -> "start"; PhotoType.END -> "end" }
        val path = "hole_${segment}_${UUID.randomUUID()}.$ext"
        return uploadToBucket(uri, holesBucketName, path)
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
            val full = publicUrl
            val marker = "/storage/v1/object/"
            val start = full.indexOf(marker)
            if (start == -1) return false
            val after = full.substring(start + marker.length) // "<visibility>/<bucket>/<object>[?...]"
            val firstSlash = after.indexOf('/')
            if (firstSlash <= 0) return false
            val afterVisibility = after.substring(firstSlash + 1) // "<bucket>/<object>[?...]"
            val secondSlash = afterVisibility.indexOf('/')
            if (secondSlash <= 0) return false
            val bucketInUrl = afterVisibility.substring(0, secondSlash)
            var objectPath = afterVisibility.substring(secondSlash + 1)
            val q = objectPath.indexOf('?')
            if (q != -1) objectPath = objectPath.substring(0, q)
            objectPath = Uri.decode(objectPath)
            if (!bucketInUrl.equals(expectedBucket, ignoreCase = true) || objectPath.isBlank()) return false
            val bucketRef = client.storage.from(bucketInUrl) // use exact bucket from URL
            // Try delete single path; if unsupported, this will throw and we return false
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
        // Return a "public" URL; if bucket is private, the UI will generate a signed URL at render time.
        return bucketRef.publicUrl(path)
    }

    suspend fun getSignedUrlForPublicUrl(url: String, expiresInSeconds: Int = 604800): String? {
        // Accepts URLs like:
        // - .../storage/v1/object/public/<bucket>/<path>
        // - .../storage/v1/object/authenticated/<bucket>/<path>
        // If URL is already signed (contains "/object/sign/" or token param), returns it as-is.
        return try {
            val uri = Uri.parse(url)
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
            val bucket = afterVisibility.substring(0, secondSlash)
            val objectPath = afterVisibility.substring(secondSlash + 1)
            if (objectPath.isBlank()) return null
            val bucketRef = client.storage.from(bucket)
            bucketRef.createSignedUrl(objectPath, expiresInSeconds.seconds)
        } catch (_: Throwable) {
            null
        }
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
        } catch (e: Exception) {
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
