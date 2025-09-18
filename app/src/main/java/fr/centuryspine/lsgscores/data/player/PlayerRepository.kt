package fr.centuryspine.lsgscores.data.player

import android.net.Uri
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import fr.centuryspine.lsgscores.utils.SupabaseStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val appPreferences: AppPreferences,
    private val storageHelper: SupabaseStorageHelper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPlayersByCurrentCity(): Flow<List<Player>> {
        return appPreferences.selectedCityIdFlow.flatMapLatest { cityId ->
            if (cityId != null) {
                playerDao.getPlayersByCityId(cityId)
            } else {
                flowOf(emptyList())
            }
        }
    }

    private fun isRemoteUrl(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        val lower = s.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) {
        val cityId = appPreferences.getSelectedCityId()
            ?: throw IllegalStateException("No city selected. Cannot add player without a selected city.")

        val finalPhotoUrl = when {
            player.photoUri.isNullOrBlank() -> null
            isRemoteUrl(player.photoUri) -> player.photoUri
            else -> storageHelper.uploadPlayerPhoto(0L, Uri.parse(player.photoUri))
        }
        playerDao.insert(player.copy(cityId = cityId, photoUri = finalPhotoUrl))
    }

    suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) {
        val existing = playerDao.getById(player.id)
        val newPhotoUrl = when {
            player.photoUri.isNullOrBlank() -> null
            isRemoteUrl(player.photoUri) -> player.photoUri
            else -> storageHelper.uploadPlayerPhoto(player.id, Uri.parse(player.photoUri))
        }
        // Update DB first
        playerDao.update(player.copy(photoUri = newPhotoUrl))
        // Best-effort delete of previous remote if changed
        val oldUrl = existing?.photoUri
        if (!oldUrl.isNullOrBlank() && oldUrl != newPhotoUrl && isRemoteUrl(oldUrl)) {
            storageHelper.deletePlayerPhotoByUrl(oldUrl)
        }
    }

    suspend fun deletePlayer(player: Player) = withContext(Dispatchers.IO) {
        // Optional: delete associated remote photo when deleting the player
        val oldUrl = playerDao.getById(player.id)?.photoUri
        playerDao.delete(player)
        if (!oldUrl.isNullOrBlank() && isRemoteUrl(oldUrl)) {
            storageHelper.deletePlayerPhotoByUrl(oldUrl)
        }
    }
}
