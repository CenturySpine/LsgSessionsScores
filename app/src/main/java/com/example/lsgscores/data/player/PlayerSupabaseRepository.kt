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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerSupabaseRepository @Inject constructor(
    private val supabase: Postgrest,
) : PlayerRepository {

    companion object {
        private const val TABLE_NAME = "players"
    }

    override fun getAllPlayers(): Flow<List<Player>> = flow {
        try {
            val playersDto = supabase
                .from(TABLE_NAME)
                .select()
                .decodeList<PlayerDto>()

            emit(playersDto.map { it.toDomainModel() })
        } catch (e: Exception) {
            // Log error or handle as needed
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) {
        try {
            val playerDto = player.toDto()
            val insertedPlayer = supabase
                .from(TABLE_NAME)
                .insert(playerDto) {
                    select(Columns.list("id"))
                }
                .decodeSingle<PlayerDto>()

            insertedPlayer.id
        } catch (e: Exception) {
            // Log error or handle as needed
            0L
        }
    }

    override suspend fun updatePlayer(player: Player):Unit =  withContext(Dispatchers.IO) {
        try {
            val playerDto = player.toDto()
            supabase
                .from(TABLE_NAME)
                .update(playerDto) {
                    filter {
                        eq("id", player.id)
                    }
                }
        } catch (e: Exception) {
            // Log error or handle as needed
        }
    }

    override suspend fun deletePlayer(player: Player) :Unit =  withContext(Dispatchers.IO) {
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