package fr.centuryspine.lsgscores.data.city

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityRepository @Inject constructor(
    private val cityDao: CityDao
) {
    fun getAllCities(): Flow<List<City>> = cityDao.getAllCities()

    suspend fun getCityById(id: Long): City? = cityDao.getCityById(id)

    suspend fun insert(city: City): Long = cityDao.insert(city)

    suspend fun update(city: City) = cityDao.update(city)
}