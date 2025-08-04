package com.example.lsgscores.data.media

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Insert
    suspend fun insert(media: Media): Long

    @Update
    suspend fun update(media: Media)

    @Delete
    suspend fun delete(media: Media)

    @Query("SELECT * FROM media WHERE sessionId = :sessionId")
    fun getMediaForSession(sessionId: Int): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getById(id: Int): Media?
}