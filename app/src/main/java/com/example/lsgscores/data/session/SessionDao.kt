package com.example.lsgscores.data.session


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getById(id: Int): Flow<Session?>

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)
}
