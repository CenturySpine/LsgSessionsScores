package com.example.lsgscores.data.media

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {

    suspend fun insert(media: Media): Long = mediaDao.insert(media)

    suspend fun update(media: Media) = mediaDao.update(media)

    suspend fun delete(media: Media) = mediaDao.delete(media)

    fun getMediaForSession(sessionId: Int): Flow<List<Media>> = mediaDao.getMediaForSession(sessionId)

    suspend fun getById(id: Int): Media? = mediaDao.getById(id)
}