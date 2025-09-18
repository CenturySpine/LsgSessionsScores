package fr.centuryspine.lsgscores.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import fr.centuryspine.lsgscores.BuildConfig
import java.io.File
import java.util.UUID

@Singleton
class SupabaseStorageHelper @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    private val playersBucket: String = BuildConfig.SUPABASE_BUCKET_PLAYERS
    private val holesBucket: String = BuildConfig.SUPABASE_BUCKET_HOLES

    // Normalize bucket names to lowercase to match Supabase REST endpoints (case-sensitive)
    private val playersBucketName = playersBucket.lowercase()
    private val holesBucketName = holesBucket.lowercase()

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
        // Supabase public URL format: <base>/storage/v1/object/public/<bucket>/<path>
        // We only delete if URL clearly targets our expected bucket.
        return try {
            val uri = Uri.parse(publicUrl)
            val path = uri.path ?: return false
            val marker = "/storage/v1/object/public/"
            val idx = path.indexOf(marker)
            if (idx == -1) return false
            val after = path.substring(idx + marker.length) // <bucket>/<path>
            val firstSlash = after.indexOf('/')
            if (firstSlash <= 0) return false
            val bucketInUrl = after.substring(0, firstSlash)
            val objectPath = after.substring(firstSlash + 1)
            if (!bucketInUrl.equals(expectedBucket, ignoreCase = true) || objectPath.isBlank()) return false
            val bucketRef = client.storage.from(expectedBucket) // always use normalized expected bucket
            // Try delete single path; if unsupported, this will throw and we return false
            bucketRef.delete(objectPath)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun uploadToBucket(uri: Uri, bucket: String, path: String): String {
        val bytes = readBytesFromUri(uri)
            ?: throw IllegalArgumentException("Unable to read image data from URI or file: $uri")

        val bucketRef = client.storage.from(bucket)
        // Upsert to avoid failures if we retry
        bucketRef.upload(path, bytes, upsert = true)
        return bucketRef.publicUrl(path)
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
