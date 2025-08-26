package fr.centuryspine.lsgscores.data.gamezone

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameZoneDao {
    @Query("SELECT * FROM game_zones ORDER BY name ASC")
    fun getAllGameZones(): Flow<List<GameZone>>

    @Query("SELECT * FROM game_zones WHERE id = :id")
    suspend fun getGameZoneById(id: Long): GameZone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gameZone: GameZone): Long

    @Update
    suspend fun update(gameZone: GameZone)

    @Delete
    suspend fun delete(gameZone: GameZone)
}
