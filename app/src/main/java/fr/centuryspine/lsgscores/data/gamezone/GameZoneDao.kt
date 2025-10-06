package fr.centuryspine.lsgscores.data.gamezone

import kotlinx.coroutines.flow.Flow

interface GameZoneDao {
    fun getGameZonesByCityId(cityId: Long): Flow<List<GameZone>>

    suspend fun getAll(): List<GameZone>

    suspend fun getGameZoneById(id: Long): GameZone?

    suspend fun insert(gameZone: GameZone): Long

    suspend fun update(gameZone: GameZone)

    suspend fun delete(gameZone: GameZone)
}
