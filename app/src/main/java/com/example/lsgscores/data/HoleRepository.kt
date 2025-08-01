package com.example.lsgscores.data

import kotlinx.coroutines.flow.Flow

class HoleRepository(private val holeDao: HoleDao) {

    fun getAllHoles(): Flow<List<Hole>> = holeDao.getAll()

    suspend fun insertHole(hole: Hole): Long = holeDao.insert(hole)

    suspend fun updateHole(hole: Hole) = holeDao.update(hole)

    suspend fun deleteHole(hole: Hole) = holeDao.delete(hole)
}
