package fr.centuryspine.lsgscores.data.hole

import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HoleRepository @Inject constructor(
    private val holeDao: HoleDao,
    private val gameZoneDao: GameZoneDao,
    private val appPreferences: AppPreferences
) {

    fun getHolesByCurrentCity(): Flow<List<Hole>> {
        val cityId = appPreferences.getSelectedCityId()
            ?: throw IllegalStateException("No city selected. Holes screen should not be accessible without a selected city.")
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
