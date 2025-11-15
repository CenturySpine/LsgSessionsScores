package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.data.authuser.CurrentUserProviderImpl
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.data.player.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    // We inject the concrete implementation to avoid adding a Hilt @Binds module
    private val currentUserProvider: CurrentUserProviderImpl
) : ViewModel() {

    val players: Flow<List<Player>> = repository.getPlayersByCurrentCity()

    suspend fun getPlayerById(id: Long): Player? = repository.getPlayerById(id)

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            repository.updatePlayer(player)
        }
    }

    fun isCurrentAuthenticatedUser(player: Player): Boolean {
        val currentUserId = currentUserProvider.userIdOrNull()
        return currentUserId != null && currentUserId == player.userId
    }


}