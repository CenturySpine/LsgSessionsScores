package fr.centuryspine.lsgscores.data.hole

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HoleDao {
    @Query("""
        SELECT h.* FROM holes h 
        INNER JOIN game_zones gz ON h.gameZoneId = gz.id 
        WHERE gz.cityId = :cityId
    """)
    fun getHolesByCityId(cityId: Long): Flow<List<Hole>>

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
