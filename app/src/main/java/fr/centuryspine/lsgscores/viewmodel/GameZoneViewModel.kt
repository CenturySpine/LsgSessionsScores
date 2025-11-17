// Replace the entire GameZoneViewModel class with:

package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.data.gamezone.GameZoneRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameZoneViewModel @Inject constructor(
    private val repository: GameZoneRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    // Reactive game zones that automatically update when city changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val gameZones: StateFlow<List<GameZone>> = appPreferences.selectedCityIdFlow
        .flatMapLatest { cityId ->
            if (cityId != null) {
                repository.getGameZonesByCityId(cityId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addGameZone(name: String) {
        viewModelScope.launch {
            val cityId = appPreferences.selectedCityIdFlow.value
                ?: throw Exception("No city selected")
            if (name.isNotBlank()) {
                val newGameZone = GameZone(
                    name = name,
                    cityId = cityId
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

    fun clearError() {
        _error.value = null
    }
}