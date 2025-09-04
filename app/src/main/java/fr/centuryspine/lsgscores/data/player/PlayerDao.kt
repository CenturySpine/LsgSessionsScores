package fr.centuryspine.lsgscores.data.player

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE cityId = :cityId")
    fun getPlayersByCityId(cityId: Long): Flow<List<Player>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(player: Player): Long

    @Update
     fun update(player: Player)

    @Delete
     fun delete(player: Player)
}
