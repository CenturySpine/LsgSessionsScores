package com.example.lsgscores.ui.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.User
import com.example.lsgscores.data.UserDatabase
import com.example.lsgscores.data.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository

    val users: Flow<List<User>>

    init {
        val db = UserDatabase.getDatabase(application)
        repository = UserRepository(db.userDao())
        users = repository.getAllUsers() // users = le Flow exposé par le repo
    }



    fun addUser(name: String, photoUri: String?, onUserAdded: () -> Unit) {
        viewModelScope.launch {
            repository.insertUser(User(name = name, photoUri = photoUri))
            onUserAdded()
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            // Delete the photo file if the path is not null or empty
            user.photoUri?.let { photoPath ->
                try {
                    val file = java.io.File(photoPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // Log or handle error if needed
                }
            }
            repository.deleteUser(user)
        }
    }


}

class UserViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
