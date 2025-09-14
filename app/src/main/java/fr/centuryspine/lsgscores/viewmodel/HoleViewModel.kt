package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.hole.HoleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HoleViewModel @Inject constructor(
    private val repository: HoleRepository
) : ViewModel() {

    val holes: Flow<List<Hole>> = repository.getHolesByCurrentCity()

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
