package fr.centuryspine.lsgscores.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.data.authuser.AppUserDaoSupabase
import fr.centuryspine.lsgscores.data.city.City
import fr.centuryspine.lsgscores.data.city.CityRepository
import fr.centuryspine.lsgscores.data.player.PlayerDao
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val appPreferences: AppPreferences,
    private val imageCacheManager: fr.centuryspine.lsgscores.utils.ImageCacheManager,
    private val appUserDao: AppUserDaoSupabase,
    private val playerDao: PlayerDao
) : ViewModel() {

    val cities = cityRepository.getAllCities()

    private val _selectedCityId = MutableStateFlow<Long?>(null)
    private val _hasCitySelected = MutableStateFlow(false)
    val hasCitySelected: StateFlow<Boolean> = _hasCitySelected.asStateFlow()

    private val _authenticatedUserCityName = MutableStateFlow<String?>(null)
    val authenticatedUserCityName: StateFlow<String?> = _authenticatedUserCityName.asStateFlow()

    init {
        // Observe changes to selectedCityIdFlow reactively
        viewModelScope.launch {
            appPreferences.selectedCityIdFlow.collect { cityId ->
                _selectedCityId.value = cityId
                _hasCitySelected.value = cityId != null
            }
        }

        // Load authenticated user's city name
        loadAuthenticatedUserCity()
    }

    private fun loadAuthenticatedUserCity() {
        viewModelScope.launch {
            try {
                val playerId = appUserDao.getLinkedPlayerId()
                if (playerId != null) {
                    val player = playerDao.getById(playerId)
                    if (player != null) {
                        val city = cityRepository.getCityById(player.cityId)
                            ?: throw Exception("City not found")
                        _authenticatedUserCityName.value = city.name
                        selectCity(city.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("CityViewModel", "loadAuthenticatedUserCity: ${e.message}", e)
                // Si une erreur se produit, on laisse la valeur à null
                _authenticatedUserCityName.value = null
            }
        }
    }

    fun selectCity(cityId: Long) {
        appPreferences.setSelectedCityId(cityId)
        _selectedCityId.value = cityId
        _hasCitySelected.value = true
        // Clear previous cache and warm new city's images in background
        viewModelScope.launch {
            imageCacheManager.clearAndWarmForCity(cityId)
        }
    }

    fun addCity(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                cityRepository.insert(City(name = name))
            }
        }
    }

    fun updateCity(city: City) {
        if (city.name.isNotBlank()) {
            viewModelScope.launch {
                cityRepository.update(city)
            }
        }
    }
}