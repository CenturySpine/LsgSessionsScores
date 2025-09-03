package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.data.city.CurrentCityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityViewModel @Inject constructor(
    private val currentCityManager: CurrentCityManager
) : ViewModel() {

    private val _hasCitySelected = MutableStateFlow(false)
    val hasCitySelected: StateFlow<Boolean> = _hasCitySelected.asStateFlow()

    init {
        checkCitySelection()
    }

    fun checkCitySelection() {
        _hasCitySelected.value = currentCityManager.hasSelectedCity()
    }
}