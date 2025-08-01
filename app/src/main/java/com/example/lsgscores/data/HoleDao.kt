package com.example.lsgscores.data

import androidx.room.*

@Dao
interface HoleDao {
    @Query("SELECT * FROM holes")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<Hole>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hole: Hole): Long

    @Update
    suspend fun update(hole: Hole)

    @Delete
    suspend fun delete(hole: Hole)
}
