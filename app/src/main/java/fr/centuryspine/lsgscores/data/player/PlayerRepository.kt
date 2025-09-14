package fr.centuryspine.lsgscores.data.player

import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val appPreferences: AppPreferences
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPlayersByCurrentCity(): Flow<List<Player>> {
        return appPreferences.selectedCityIdFlow.flatMapLatest { cityId ->
            if (cityId != null) {
                playerDao.getPlayersByCityId(cityId)
            } else {
                flowOf(emptyList())
            }
        }
    }
    
    suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) { 
        val cityId = appPreferences.getSelectedCityId()
            ?: throw IllegalStateException("No city selected. Cannot add player without a selected city.")
        playerDao.insert(player.copy(cityId = cityId)) 
    }
    
    suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) { playerDao.update(player) }
    suspend fun deletePlayer(player: Player) = withContext(Dispatchers.IO) { playerDao.delete(player) }
}
