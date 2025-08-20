package com.example.lsgscores.data.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


interface PlayerRepository {
    fun getAllPlayers(): Flow<List<Player>>
    fun getPlayerById(id: Long): Flow<Player?>
    suspend fun insertPlayer(player: Player): Long
    suspend fun updatePlayer(player: Player)
    suspend fun deletePlayer(player: Player)
}





@Singleton
class PlayerRoomRepository @Inject constructor(
    private val playerDao: PlayerDao
) : PlayerRepository {

    override fun getAllPlayers(): Flow<List<Player>> = playerDao.getAll()
    override fun getPlayerById(id: Long): Flow<Player?> {
        TODO("Not yet implemented")
    }

    override suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) {
        playerDao.insert(player)
    }

    override suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) {
        playerDao.update(player)
    }

    override suspend fun deletePlayer(player: Player) = withContext(Dispatchers.IO) {
        playerDao.delete(player)
    }
}