package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.data.gamezone.GameZoneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameZoneViewModel @Inject constructor(
    private val repository: GameZoneRepository  // Use repository instead of DAO
) : ViewModel() {

    private val _gameZones = MutableStateFlow<List<GameZone>>(emptyList())
    val gameZones: StateFlow<List<GameZone>> = _gameZones.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllGameZones().collect { zones ->
                _gameZones.value = zones
            }
        }
    }

    fun addGameZone(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val newGameZone = GameZone(name = name)
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