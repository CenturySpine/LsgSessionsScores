package fr.centuryspine.lsgscores.data.player

import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val appPreferences: AppPreferences
) {
    fun getPlayersByCurrentCity(): Flow<List<Player>> {
        val cityId = appPreferences.getSelectedCityId()
            ?: throw IllegalStateException("No city selected. Players screen should not be accessible without a selected city.")
        return playerDao.getPlayersByCityId(cityId)
    }
    
    suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) { 
        val cityId = appPreferences.getSelectedCityId()
            ?: throw IllegalStateException("No city selected. Cannot add player without a selected city.")
        playerDao.insert(player.copy(cityId = cityId)) 
    }
    
    suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) { playerDao.update(player) }
    suspend fun deletePlayer(player: Player) = withContext(Dispatchers.IO) { playerDao.delete(player) }
}
