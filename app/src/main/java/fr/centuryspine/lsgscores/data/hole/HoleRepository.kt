package fr.centuryspine.lsgscores.data.hole

import androidx.core.net.toUri
import fr.centuryspine.lsgscores.data.gamezone.GameZoneDao
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import fr.centuryspine.lsgscores.utils.SupabaseStorageHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class HoleRepository @Inject constructor(
    private val holeDao: HoleDao,
    private val gameZoneDao: GameZoneDao,
    private val appPreferences: AppPreferences,
    private val storageHelper: SupabaseStorageHelper,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHolesByCurrentCity(): Flow<List<Hole>> {
        return appPreferences.selectedCityIdFlow.flatMapLatest { cityId ->
            if (cityId != null) {
                holeDao.getHolesByCityId(cityId)
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

    fun getHolesByCityId(cityId: Long): Flow<List<Hole>> {
        return holeDao.getHolesByCityId(cityId)
    }

    suspend fun insertHole(hole: Hole): Long = withContext(Dispatchers.IO) {
        // Validate that GameZone exists
        val gameZone = gameZoneDao.getGameZoneById(hole.gameZoneId)
        if (gameZone == null) {
            throw IllegalArgumentException("GameZone with id ${hole.gameZoneId} does not exist")
        }
        val startUri = hole.startPhotoUri
        val endUri = hole.endPhotoUri
        val result = coroutineScope {
            val startDeferred = async {
                when {
                    startUri.isNullOrBlank() -> null
                    isRemoteUrl(startUri) -> startUri
                    else -> storageHelper.uploadHolePhoto(SupabaseStorageHelper.PhotoType.START, startUri.toUri())
                }
            }
            val endDeferred = async {
                when {
                    endUri.isNullOrBlank() -> null
                    isRemoteUrl(endUri) -> endUri
                    else -> storageHelper.uploadHolePhoto(SupabaseStorageHelper.PhotoType.END, endUri.toUri())
                }
            }
            val finalStart = startDeferred.await()
            val finalEnd = endDeferred.await()
            val id = holeDao.insert(hole.copy(startPhotoUri = finalStart, endPhotoUri = finalEnd))
            id
        }
        result
    }

    suspend fun updateHole(hole: Hole) = withContext(Dispatchers.IO) {
        // Validate that GameZone exists
        val gameZone = gameZoneDao.getGameZoneById(hole.gameZoneId)
        if (gameZone == null) {
            throw IllegalArgumentException("GameZone with id ${hole.gameZoneId} does not exist")
        }
        // Load existing to know previous URLs
        val existing = holeDao.getById(hole.id).firstOrNull()
        val newStartEnd = coroutineScope {
            val startDeferred = async {
                when {
                    hole.startPhotoUri.isNullOrBlank() -> null
                    isRemoteUrl(hole.startPhotoUri) -> hole.startPhotoUri
                    else -> storageHelper.uploadHolePhoto(
                        SupabaseStorageHelper.PhotoType.START,
                        hole.startPhotoUri.toUri()
                    )
                }
            }
            val endDeferred = async {
                when {
                    hole.endPhotoUri.isNullOrBlank() -> null
                    isRemoteUrl(hole.endPhotoUri) -> hole.endPhotoUri
                    else -> storageHelper.uploadHolePhoto(SupabaseStorageHelper.PhotoType.END, hole.endPhotoUri.toUri())
                }
            }
            Pair(startDeferred.await(), endDeferred.await())
        }
        val updated = hole.copy(startPhotoUri = newStartEnd.first, endPhotoUri = newStartEnd.second)
        // Update DB first
        holeDao.update(updated)

        // Best-effort deletions for changed URLs
        val oldStart = existing?.startPhotoUri
        val oldEnd = existing?.endPhotoUri
        val newStart = updated.startPhotoUri
        val newEnd = updated.endPhotoUri
        if (!oldStart.isNullOrBlank() && oldStart != newStart && isRemoteUrl(oldStart)) {
            storageHelper.deleteHolePhotoByUrl(oldStart)
        }
        if (!oldEnd.isNullOrBlank() && oldEnd != newEnd && isRemoteUrl(oldEnd)) {
            storageHelper.deleteHolePhotoByUrl(oldEnd)
        }
    }

    suspend fun deleteHole(hole: Hole) = withContext(Dispatchers.IO) {
        val existing = holeDao.getById(hole.id).firstOrNull()
        holeDao.delete(hole)
        val oldStart = existing?.startPhotoUri
        val oldEnd = existing?.endPhotoUri
        if (!oldStart.isNullOrBlank() && isRemoteUrl(oldStart)) storageHelper.deleteHolePhotoByUrl(oldStart)
        if (!oldEnd.isNullOrBlank() && isRemoteUrl(oldEnd)) storageHelper.deleteHolePhotoByUrl(oldEnd)
    }

    fun getHoleById(id: Long): Flow<Hole> = holeDao.getById(id)

}
