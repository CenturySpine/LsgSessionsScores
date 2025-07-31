package com.example.lsgscores.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {
     fun getAllUsers(): Flow<List<User>> =  userDao.getAll()
    suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) { userDao.insert(user) }
    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) { userDao.update(user) }
    suspend fun deleteUser(user: User) = withContext(Dispatchers.IO) { userDao.delete(user) }
}
