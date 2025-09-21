package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.data.city.City
import fr.centuryspine.lsgscores.data.city.CityRepository
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
    private val imageCacheManager: fr.centuryspine.lsgscores.utils.ImageCacheManager
) : ViewModel() {

    val cities = cityRepository.getAllCities()

    private val _selectedCityId = MutableStateFlow<Long?>(null)
    val selectedCityId: StateFlow<Long?> = _selectedCityId.asStateFlow()
    private val _hasCitySelected = MutableStateFlow(false)
    val hasCitySelected: StateFlow<Boolean> = _hasCitySelected.asStateFlow()

    init {
        // Observe changes to selectedCityIdFlow reactively
        viewModelScope.launch {
            appPreferences.selectedCityIdFlow.collect { cityId ->
                _selectedCityId.value = cityId
                _hasCitySelected.value = cityId != null
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