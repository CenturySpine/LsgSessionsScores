package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.data.player.PlayerRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _players = MutableStateFlow<Flow<List<Player>>>(emptyFlow())
    val players: Flow<List<Player>> 
        get() = _players.value

    init {
        loadPlayers()
    }

    fun loadPlayers() {
        _players.value = repository.getPlayersByCurrentCity()
    }

    fun refreshPlayers() {
        loadPlayers()
    }

    fun addPlayer(name: String, photoUri: String?, onPlayerAdded: () -> Unit) {
        viewModelScope.launch {
            repository.insertPlayer(Player(name = name, photoUri = photoUri))
            onPlayerAdded()
        }
    }
    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            repository.updatePlayer(player)
        }
    }
    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            // Delete the photo file if the path is not null or empty
            player.photoUri?.let { photoPath ->
                try {
                    val file = File(photoPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {
                    // Log or handle error if needed
                }
            }
            repository.deletePlayer(player)
        }
    }


}