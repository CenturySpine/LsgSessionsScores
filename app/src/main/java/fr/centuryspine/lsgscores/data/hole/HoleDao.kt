package fr.centuryspine.lsgscores.data.hole

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HoleDao {
    @Query("SELECT * FROM holes")
    fun getAll(): Flow<List<Hole>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hole: Hole): Long

    @Update
    suspend fun update(hole: Hole)

    @Delete
    suspend fun delete(hole: Hole)

    @Query("SELECT * FROM holes WHERE id = :id")
    fun getById(id: Long): Flow<Hole>
    @Query("SELECT * FROM holes WHERE gameZoneId = :gameZoneId")
    suspend fun getHolesByGameZoneId(gameZoneId: Long): List<Hole>
}
