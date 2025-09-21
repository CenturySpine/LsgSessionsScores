package fr.centuryspine.lsgscores.data.player

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE cityId = :cityId")
    fun getPlayersByCityId(cityId: Long): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE cityId = :cityId")
    suspend fun getPlayersByCityIdList(cityId: Long): List<Player>

    @Query("SELECT * FROM players")
    suspend fun getAll(): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: Long): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(player: Player): Long

    @Update
    fun update(player: Player)

    @Delete
    fun delete(player: Player)
}
