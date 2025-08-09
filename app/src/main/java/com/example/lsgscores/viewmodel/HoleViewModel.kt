package com.example.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.hole.HoleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HoleViewModel @Inject constructor(
    private val repository: HoleRepository
) : ViewModel() {

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
