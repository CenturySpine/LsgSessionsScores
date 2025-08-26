package fr.centuryspine.lsgscores.data.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlayerRepository(private val playerDao: PlayerDao) {
     fun getAllPlayers(): Flow<List<Player>> =  playerDao.getAll()
    suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) { playerDao.insert(player) }
    suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) { playerDao.update(player) }
    suspend fun deletePlayer(player: Player) = withContext(Dispatchers.IO) { playerDao.delete(player) }
}
