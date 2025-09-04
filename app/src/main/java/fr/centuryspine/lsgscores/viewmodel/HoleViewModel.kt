package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.hole.HoleRepository
import fr.centuryspine.lsgscores.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HoleViewModel @Inject constructor(
    private val repository: HoleRepository
) : ViewModel() {

    private val _holes = MutableStateFlow<Flow<List<Hole>>>(emptyFlow())
    val holes: Flow<List<Hole>>
        get() = _holes.value

    init {
        loadHoles()
    }

    fun loadHoles() {
        _holes.value = repository.getHolesByCurrentCity()
    }

    fun refreshHoles() {
        loadHoles()
    }

    fun getHoleById(id: Long): Flow<Hole> {
        return repository.getHoleById(id)
    }

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
