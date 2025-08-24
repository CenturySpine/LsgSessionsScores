package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.gamezone.GameZone
import com.example.lsgscores.data.gamezone.GameZoneDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameZoneViewModel @Inject constructor(
    private val gameZoneDao: GameZoneDao
) : ViewModel() {

    private val _gameZones = MutableStateFlow<List<GameZone>>(emptyList())
    val gameZones: StateFlow<List<GameZone>> = _gameZones.asStateFlow()

    init {
        viewModelScope.launch {
            gameZoneDao.getAllGameZones().collect { zones ->
                _gameZones.value = zones
            }
        }
    }

    fun addGameZone(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val newGameZone = GameZone(name = name)
                gameZoneDao.insert(newGameZone)
            }
        }
    }

    fun deleteGameZone(gameZone: GameZone) {
        viewModelScope.launch {
            gameZoneDao.delete(gameZone)
        }
    }

    fun getGameZoneById(id: Long) = viewModelScope.launch {
        gameZoneDao.getGameZoneById(id)
    }
}
