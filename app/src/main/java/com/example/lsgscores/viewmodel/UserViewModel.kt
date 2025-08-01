package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.User
import com.example.lsgscores.data.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {


    val users: Flow<List<User>> = repository.getAllUsers()


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
                    val file = File(photoPath)
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

class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
