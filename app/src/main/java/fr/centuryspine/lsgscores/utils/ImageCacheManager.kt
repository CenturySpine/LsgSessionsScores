package fr.centuryspine.lsgscores.utils

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.centuryspine.lsgscores.data.hole.HoleDao
import fr.centuryspine.lsgscores.data.player.PlayerDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized image cache manager that warms (prefetches) remote images into Coil's caches
 * and can clear caches when needed.
 *
 * Responsibilities:
 * - Warm cache for all players and holes of a city in background.
 * - Warm cache for individual entities on CRUD updates.
 * - Clear cache on app close and when city changes.
 */
@Singleton
class ImageCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageHelper: SupabaseStorageHelper,
    private val playerDao: PlayerDao,
    private val holeDao: HoleDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun imageLoader(): ImageLoader = context.imageLoader

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache() {
        try {
            Log.i("ImageCache", "Clearing image caches (memory + disk)")
            imageLoader().memoryCache?.clear()
            imageLoader().diskCache?.clear()
            Log.i("ImageCache", "Caches cleared")
        } catch (t: Throwable) {
            Log.w("ImageCache", "Failed to clear cache", t)
        }
    }

    fun clearAndWarmForCity(cityId: Long) {
        Log.i("ImageCache", "clearAndWarmForCity(cityId=$cityId)")
        clearCache()
        warmAllForCity(cityId)
    }

    fun warmAllForCity(cityId: Long) {
        scope.launch {
            try {
                // Players
                val players = try { playerDao.getPlayersByCityIdList(cityId) } catch (_: Throwable) { emptyList() }
                val playerUrls = players.mapNotNull { it.photoUri }.filter { isRemoteUrl(it) }
                val playersCount = playerUrls.size

                // Holes
                val holes = try { holeDao.getHolesByCityIdList(cityId) } catch (_: Throwable) { emptyList() }
                var holeStartCount = 0
                var holeEndCount = 0
                val holeUrls = buildList<String> {
                    holes.forEach { h ->
                        h.startPhotoUri?.let { if (isRemoteUrl(it)) { add(it); holeStartCount++ } }
                        h.endPhotoUri?.let { if (isRemoteUrl(it)) { add(it); holeEndCount++ } }
                    }
                }
                val total = playersCount + holeStartCount + holeEndCount
                Log.i("ImageCache", "WarmAllForCity(cityId=$cityId): players=$playersCount, holeStart=$holeStartCount, holeEnd=$holeEndCount, total=$total")

                warmUrls(playerUrls + holeUrls)
            } catch (t: Throwable) {
                Log.w("ImageCache", "warmAllForCity failed", t)
            }
        }
    }

    fun warmPlayerPhoto(url: String?) {
        if (url.isNullOrBlank() || !isRemoteUrl(url)) {
            Log.d("ImageCache", "WarmPlayerPhoto: skipped (null or non-remote)")
            return
        }
        Log.i("ImageCache", "WarmPlayerPhoto: players=1, total=1")
        scope.launch { warmUrls(listOf(url)) }
    }

    fun warmHolePhotos(startUrl: String?, endUrl: String?) {
        val startCount = if (!startUrl.isNullOrBlank() && isRemoteUrl(startUrl)) 1 else 0
        val endCount = if (!endUrl.isNullOrBlank() && isRemoteUrl(endUrl)) 1 else 0
        val urls = listOfNotNull(startUrl, endUrl).filter { isRemoteUrl(it) }
        if (urls.isEmpty()) {
            Log.d("ImageCache", "WarmHolePhotos: skipped (no remote URLs)")
            return
        }
        Log.i("ImageCache", "WarmHolePhotos: holeStart=$startCount, holeEnd=$endCount, total=${startCount + endCount}")
        scope.launch { warmUrls(urls) }
    }

    private suspend fun warmUrls(urls: List<String>) {
        if (urls.isEmpty()) return
        val loader = imageLoader()
        val distinct = urls.distinct()
        Log.d("ImageCache", "Enqueue prefetch for ${distinct.size} distinct URL(s)")
        distinct.forEach { rawUrl ->
            val signed = storageHelper.getSignedUrlForPublicUrl(rawUrl) ?: rawUrl
            val key = ImageCacheKeyHelper.stableCacheKey(rawUrl)
            val request = ImageRequest.Builder(context)
                .data(signed)
                .memoryCacheKey(key)
                .diskCacheKey(key)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build()
            try {
                // Enqueue prefetch; we don't need the result here
                loader.enqueue(request)
            } catch (t: Throwable) {
                Log.d("ImageCache", "Prefetch failed for $rawUrl", t)
            }
        }
    }

    private fun isRemoteUrl(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        val lower = s.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }
}
