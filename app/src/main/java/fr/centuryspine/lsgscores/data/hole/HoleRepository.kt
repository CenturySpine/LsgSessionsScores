package fr.centuryspine.lsgscores.data.hole

import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class HoleRepository @Inject constructor(
    private val holeDao: HoleDao,
    private val gameZoneDao: GameZoneDao,
    private val appPreferences: AppPreferences
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHolesByCurrentCity(): Flow<List<Hole>> {
        return appPreferences.selectedCityIdFlow.flatMapLatest { cityId ->
            if (cityId != null) {
                holeDao.getHolesByCityId(cityId)
            } else {
                flowOf(emptyList())
            }
        }
    }

    fun getHolesByCityId(cityId: Long): Flow<List<Hole>> {
        return holeDao.getHolesByCityId(cityId)
    }

    suspend fun insertHole(hole: Hole): Long {
        // Validate that GameZone exists
        val gameZone = gameZoneDao.getGameZoneById(hole.gameZoneId)
        if (gameZone == null) {
            throw IllegalArgumentException("GameZone with id ${hole.gameZoneId} does not exist")
        }
        return holeDao.insert(hole)
    }

    suspend fun updateHole(hole: Hole) {
        // Validate that GameZone exists
        val gameZone = gameZoneDao.getGameZoneById(hole.gameZoneId)
        if (gameZone == null) {
            throw IllegalArgumentException("GameZone with id ${hole.gameZoneId} does not exist")
        }
        holeDao.update(hole)
    }

    suspend fun deleteHole(hole: Hole) = holeDao.delete(hole)

    fun getHoleById(id: Long): Flow<Hole> = holeDao.getById(id)


}
