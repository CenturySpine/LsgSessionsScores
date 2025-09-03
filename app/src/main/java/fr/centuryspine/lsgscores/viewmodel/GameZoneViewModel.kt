// Replace the entire GameZoneViewModel class with:

package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.data.gamezone.GameZoneRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameZoneViewModel @Inject constructor(
    private val repository: GameZoneRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _gameZones = MutableStateFlow<List<GameZone>>(emptyList())
    val gameZones: StateFlow<List<GameZone>> = _gameZones.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasSelectedCity = MutableStateFlow(false)
    val hasSelectedCity: StateFlow<Boolean> = _hasSelectedCity.asStateFlow()

    private val _selectedCityId = MutableStateFlow<Long?>(null)
    val selectedCityId: StateFlow<Long?> = _selectedCityId.asStateFlow()

    init {
        loadGameZones()
    }

    private fun loadGameZones() {
        viewModelScope.launch {
            val cityId = appPreferences.getSelectedCityId()
            _selectedCityId.value = cityId
            _hasSelectedCity.value = cityId != null

            if (cityId != null) {
                repository.getGameZonesByCityId(cityId).collect { zones ->
                    _gameZones.value = zones
                }
            } else {
                _gameZones.value = emptyList()
            }
        }
    }

    fun refreshGameZones() {
        loadGameZones()
    }

    fun addGameZone(name: String) {
        val cityId = appPreferences.getSelectedCityId()
        if (name.isNotBlank() && cityId != null) {
            viewModelScope.launch {
                val newGameZone = GameZone(
                    name = name,
                    cityId = cityId  // Use selected city instead of default
                )
                repository.insert(newGameZone)
            }
        }
    }

    fun updateGameZone(gameZone: GameZone) {
        viewModelScope.launch {
            repository.update(gameZone)
        }
    }

    fun deleteGameZone(gameZone: GameZone) {
        viewModelScope.launch {
            try {
                repository.delete(gameZone)
            } catch (e: IllegalStateException) {
                _error.value = e.message
            }
        }
    }
}