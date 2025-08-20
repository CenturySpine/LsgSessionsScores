package com.example.lsgscores.data.player

import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerPhotoService @Inject constructor(
    private val storage: Storage
) {
    companion object {
        private const val BUCKET_NAME = "player-photos"
    }

    suspend fun uploadPlayerPhoto(playerId: Long, photoFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "player_${playerId}_${System.currentTimeMillis()}.jpg"
            val bucket = storage.from(BUCKET_NAME)

            // Upload the file
            bucket.upload(fileName, photoFile.readBytes())

            // Get public URL
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            println("❌ DEBUG: Upload failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun deletePlayerPhoto(photoUrl: String) = withContext(Dispatchers.IO) {
        try {
            // Extract filename from URL
            val fileName = photoUrl.substringAfterLast("/")
            val bucket = storage.from(BUCKET_NAME)
            bucket.delete(fileName)
        } catch (e: Exception) {
            // Log error
        }
    }
}