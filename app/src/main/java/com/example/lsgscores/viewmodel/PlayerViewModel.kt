package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.player.Player
import com.example.lsgscores.data.player.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class PlayerViewModel(
    private val repository: PlayerRepository
) : ViewModel() {


    val players: Flow<List<Player>> = repository.getAllPlayers()


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
                } catch (e: Exception) {
                    // Log or handle error if needed
                }
            }
            repository.deletePlayer(player)
        }
    }


}

class playerViewModelFactory(private val repository: PlayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
