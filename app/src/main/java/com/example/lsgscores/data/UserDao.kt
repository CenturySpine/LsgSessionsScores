package com.example.lsgscores.data

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getAll(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}
