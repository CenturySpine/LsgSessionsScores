package com.example.lsgscores.data.user

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
     fun getAll(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(user: User): Long

    @Update
     fun update(user: User)

    @Delete
     fun delete(user: User)
}
