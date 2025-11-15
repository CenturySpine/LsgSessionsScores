package fr.centuryspine.lsgscores.data.player

import androidx.core.net.toUri
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
    private val storageHelper: SupabaseStorageHelper,
    private val imageCacheManager: fr.centuryspine.lsgscores.utils.ImageCacheManager
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

    suspend fun getPlayerById(id: Long): Player? = withContext(Dispatchers.IO) {
        playerDao.getById(id)
    }

    private fun isRemoteUrl(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        val lower = s.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) {
        val existing = playerDao.getById(player.id)
        val newPhotoUrl = when {
            player.photoUri.isNullOrBlank() -> null
            isRemoteUrl(player.photoUri) -> player.photoUri
            else -> storageHelper.uploadPlayerPhoto(player.photoUri.toUri())
        }
        // Update DB first
        playerDao.update(player.copy(photoUri = newPhotoUrl))
        // Best-effort delete of previous remote if changed
        val oldUrl = existing?.photoUri
        if (!oldUrl.isNullOrBlank() && oldUrl != newPhotoUrl && isRemoteUrl(oldUrl)) {
            storageHelper.deletePlayerPhotoByUrl(oldUrl)
        }
        if (!newPhotoUrl.isNullOrBlank() && isRemoteUrl(newPhotoUrl)) {
            imageCacheManager.warmPlayerPhoto(newPhotoUrl)
        }
    }

    suspend fun insertPlayer(player: Player): Long = withContext(Dispatchers.IO) {
        val cityId = appPreferences.getSelectedCityId()
            ?: 1

        val finalPhotoUrl = when {
            player.photoUri.isNullOrBlank() -> null
            isRemoteUrl(player.photoUri) -> player.photoUri
            else -> storageHelper.uploadPlayerPhoto(player.photoUri.toUri())
        }
        val id = playerDao.insert(player.copy(cityId = cityId, photoUri = finalPhotoUrl))
        if (!finalPhotoUrl.isNullOrBlank() && isRemoteUrl(finalPhotoUrl)) {
            imageCacheManager.warmPlayerPhoto(finalPhotoUrl)
        }
        id
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
