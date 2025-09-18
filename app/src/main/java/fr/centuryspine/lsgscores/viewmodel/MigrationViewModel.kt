package fr.centuryspine.lsgscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.centuryspine.lsgscores.utils.MigrationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val migrationManager: MigrationManager
) : ViewModel() {

    data class UiState(
        val isRunning: Boolean = false,
        val lastMessage: String? = null,
        val report: MigrationManager.MigrationReport? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun runMigration() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true, lastMessage = "Migration en cours…", error = null)
        viewModelScope.launch {
            try {
                val report = migrationManager.migrateAll { msg ->
                    _state.value = _state.value.copy(lastMessage = msg)
                }
                _state.value = UiState(
                    isRunning = false,
                    lastMessage = "Migration terminée",
                    report = report,
                    error = null
                )
            } catch (t: Throwable) {
                _state.value = UiState(
                    isRunning = false,
                    lastMessage = "Migration échouée",
                    report = null,
                    error = t.message ?: t.toString()
                )
            }
        }
    }
}
