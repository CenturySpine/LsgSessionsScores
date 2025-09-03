package fr.centuryspine.lsgscores.data.hole

import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import kotlinx.coroutines.flow.Flow

class HoleRepository(private val holeDao: HoleDao, private val gameZoneDao: GameZoneDao) {

    fun getAllHoles(): Flow<List<Hole>> = holeDao.getAll()

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
