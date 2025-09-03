package fr.centuryspine.lsgscores.data.city

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    // Add these methods:
    @Query("SELECT * FROM cities ORDER BY name ASC")
    fun getAllCities(): Flow<List<City>>

    @Query("SELECT * FROM cities WHERE id = :cityId")
    suspend fun getCityById(cityId: Long): City?
}