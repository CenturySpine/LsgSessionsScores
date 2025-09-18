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
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID

@Singleton
class SupabaseStorageHelper @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    private val playersBucket: String = BuildConfig.SUPABASE_BUCKET_PLAYERS
    private val holesBucket: String = BuildConfig.SUPABASE_BUCKET_HOLES

    suspend fun uploadPlayerPhoto(playerId: Long, uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val path = "player_${UUID.randomUUID()}.$ext"
        return uploadToBucket(uri, playersBucket, path)
    }

    suspend fun uploadHolePhoto(holeId: Long, type: PhotoType, uri: Uri): String {
        val ext = detectExtension(context.contentResolver, uri) ?: "jpg"
        val segment = when (type) { PhotoType.START -> "start"; PhotoType.END -> "end" }
        val path = "hole_${segment}_${UUID.randomUUID()}.$ext"
        return uploadToBucket(uri, holesBucket, path)
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
