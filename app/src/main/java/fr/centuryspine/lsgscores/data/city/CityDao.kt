package fr.centuryspine.lsgscores.data.city

import kotlinx.coroutines.flow.Flow

interface CityDao {
    fun getAllCities(): Flow<List<City>>

    suspend fun getAllList(): List<City>

    suspend fun getCityById(cityId: Long): City?

    suspend fun insert(city: City): Long

    suspend fun update(city: City)
}