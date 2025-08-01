package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.Hole
import com.example.lsgscores.data.HoleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HoleViewModel(private val repository: HoleRepository) : ViewModel() {

    val holes: Flow<List<Hole>> = repository.getAllHoles()

    fun addHole(hole: Hole, onAdded: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.insertHole(hole)
            onAdded?.invoke()
        }
    }

    fun updateHole(hole: Hole, onUpdated: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateHole(hole)
            onUpdated?.invoke()
        }
    }

    fun deleteHole(hole: Hole, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteHole(hole)
            onDeleted?.invoke()
        }
    }
}


class HoleViewModelFactory(private val repository: HoleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HoleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HoleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
