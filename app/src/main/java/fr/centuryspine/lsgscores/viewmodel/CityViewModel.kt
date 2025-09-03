package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.data.city.CityRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CityViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    val cities = cityRepository.getAllCities()

    private val _selectedCityId = MutableStateFlow<Long?>(null)
    val selectedCityId: StateFlow<Long?> = _selectedCityId.asStateFlow()
    private val _hasCitySelected = MutableStateFlow(false)
    val hasCitySelected: StateFlow<Boolean> = _hasCitySelected.asStateFlow()

    init {
        val cityId = appPreferences.getSelectedCityId()
        _selectedCityId.value = cityId
        _hasCitySelected.value = cityId != null
    }

    fun selectCity(cityId: Long) {
        appPreferences.setSelectedCityId(cityId)
        _selectedCityId.value = cityId
        _hasCitySelected.value = true  // Ajouter cette ligne
    }
}