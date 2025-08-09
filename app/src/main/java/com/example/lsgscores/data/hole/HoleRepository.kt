package com.example.lsgscores.data.hole

import kotlinx.coroutines.flow.Flow

class HoleRepository(private val holeDao: HoleDao) {

    fun getAllHoles(): Flow<List<Hole>> = holeDao.getAll()

    suspend fun insertHole(hole: Hole): Long = holeDao.insert(hole)

    suspend fun updateHole(hole: Hole) = holeDao.update(hole)

    suspend fun deleteHole(hole: Hole) = holeDao.delete(hole)

    suspend fun getHoleById(id: Long): Hole? = holeDao.getById(id)
}
