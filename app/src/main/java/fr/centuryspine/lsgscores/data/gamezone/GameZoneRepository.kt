package fr.centuryspine.lsgscores.data.gamezone

import dagger.hilt.android.scopes.ViewModelScoped
import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import fr.centuryspine.lsgscores.data.session.SessionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ViewModelScoped
class GameZoneRepository @Inject constructor(
    private val gameZoneDao: GameZoneDao,
    private val holeDao: HoleDao,
    private val sessionDao: SessionDao,
    private val appPreferences: AppPreferences
) {

    fun getGameZonesByCityId(cityId: Long): Flow<List<GameZone>> =
        gameZoneDao.getGameZonesByCityId(cityId)

    suspend fun insert(gameZone: GameZone): Long = gameZoneDao.insert(gameZone)

    suspend fun update(gameZone: GameZone) = gameZoneDao.update(gameZone)

    /**
     * Delete a GameZone with validation.
     * Throws exception if GameZone is still referenced.
     */
    suspend fun delete(gameZone: GameZone) {
        // Check if any holes reference this GameZone
        val holesUsingZone = holeDao.getHolesByGameZoneId(gameZone.id)
        if (holesUsingZone.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot delete GameZone '${gameZone.name}': " +
                        "${holesUsingZone.size} holes still reference it"
            )
        }

        // Check if any sessions reference this GameZone
        val sessionsUsingZone = sessionDao.getSessionsByGameZoneId(gameZone.id)
        if (sessionsUsingZone.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot delete GameZone '${gameZone.name}': " +
                        "${sessionsUsingZone.size} sessions still reference it"
            )
        }

        gameZoneDao.delete(gameZone)
    }
}