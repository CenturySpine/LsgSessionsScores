package fr.centuryspine.lsgscores.data.hole

import kotlinx.coroutines.flow.Flow

interface HoleDao {
    fun getHolesByCityId(cityId: Long): Flow<List<Hole>>

    suspend fun getHolesByCityIdList(cityId: Long): List<Hole>

    suspend fun getAll(): List<Hole>

    suspend fun insert(hole: Hole): Long

    suspend fun update(hole: Hole)

    suspend fun delete(hole: Hole)

    fun getById(id: Long): Flow<Hole>

    suspend fun getHolesByGameZoneId(gameZoneId: Long): List<Hole>
}
