package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.data.player.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository
) : ViewModel() {

    val players: Flow<List<Player>> = repository.getPlayersByCurrentCity()

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
    fun deletePlayer(player: Player, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deletePlayer(player)
                onSuccess()
            } catch (_: Exception) {
                // Swallow for now; could expose error callback in the future
            }
        }
    }


}