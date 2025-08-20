package com.example.lsgscores.data.player

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.example.lsgscores.data.PlayerDto
import com.example.lsgscores.data.toDomainModel
import com.example.lsgscores.data.toDto
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerSupabaseRepository @Inject constructor(
    private val supabase: Postgrest,
    private val photoService: PlayerPhotoService
) : PlayerRepository {

    companion object {
        private const val TABLE_NAME = "players"
    }

    override fun getAllPlayers(): Flow<List<Player>> = flow {
        try {
            println("🔍 DEBUG: Fetching all players from Supabase")
            val playersDto = supabase
                .from(TABLE_NAME)
                .select()
                .decodeList<PlayerDto>()

            println("🔍 DEBUG: Received ${playersDto.size} players from DB")
            playersDto.forEach { dto ->
                println("🔍 DEBUG: Player ${dto.name} - photoUri: ${dto.photoUrl}, photoUrl: ${dto.photoUrl}")
            }

            val domainPlayers = playersDto.map { it.toDomainModel() }
            domainPlayers.forEach { player ->
                println("🔍 DEBUG: Domain Player ${player.name} - photoUri: ${player.photoUri}")
            }

            emit(domainPlayers)
        } catch (e: Exception) {
            println("❌ DEBUG: getAllPlayers failed: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) {
        try {
            println("🔍 DEBUG: Starting insertPlayer for: ${player.name}")

            // 1. Insert player first to get ID
            val playerDtoWithoutPhoto = player.copy(photoUri = null).toDto()

            // Insertion avec récupération de l'objet complet
            val insertedPlayer = supabase
                .from(TABLE_NAME)
                .insert(playerDtoWithoutPhoto) {
                    select() // Récupérer tous les champs, pas juste l'ID
                }
                .decodeSingle<PlayerDto>()

            println("🔍 DEBUG: Player inserted with ID: ${insertedPlayer.id}")

            // 2. Upload photo if exists
            val photoUrl = player.photoUri?.let { localPath ->
                println("🔍 DEBUG: Uploading photo from: $localPath")
                val photoFile = File(localPath)
                if (photoFile.exists()) {
                    photoService.uploadPlayerPhoto(insertedPlayer.id, photoFile)
                } else {
                    println("❌ DEBUG: Photo file doesn't exist: $localPath")
                    null
                }
            }

            // 3. Update player with photo URL if upload successful
            if (photoUrl != null) {
                println("🔍 DEBUG: Updating player with photo URL: $photoUrl")
                supabase
                    .from(TABLE_NAME)
                    .update(mapOf("photo_url" to photoUrl)) {
                        filter {
                            eq("id", insertedPlayer.id)
                        }
                    }
                println("🔍 DEBUG: Player updated with photo URL")
            }

            insertedPlayer.id
        } catch (e: Exception) {
            println("❌ DEBUG: insertPlayer failed: ${e.message}")
            e.printStackTrace()
            0L
        }
    }

    override suspend fun updatePlayer(player: Player): Unit = withContext(Dispatchers.IO) {
        try {
            // Handle photo upload first
            val finalPhotoUrl = player.photoUri?.let { uri ->
                if (uri.startsWith("http")) {
                    uri // Keep existing URL
                } else {
                    // Upload new photo
                    val photoFile = File(uri)
                    if (photoFile.exists()) {
                        photoService.uploadPlayerPhoto(player.id, photoFile)
                    } else null
                }
            }

            // Simple string-based update
            supabase
                .from(TABLE_NAME)
                .update(
                    mapOf(
                        "name" to player.name,
                        "photo_url" to finalPhotoUrl
                    )
                ) {
                    filter {
                        eq("id", player.id)
                    }
                }

        } catch (e: Exception) {
            println("❌ DEBUG: updatePlayer failed: ${e.message}")
            e.printStackTrace()
        }
    }
    override suspend fun deletePlayer(player: Player): Unit = withContext(Dispatchers.IO) {
        try {
            supabase
                .from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", player.id)
                    }
                }
        } catch (e: Exception) {
            // Log error or handle as needed
        }
    }
}