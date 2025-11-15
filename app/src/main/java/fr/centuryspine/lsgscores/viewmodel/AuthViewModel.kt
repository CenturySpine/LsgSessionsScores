package fr.centuryspine.lsgscores.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val appUserDao: fr.centuryspine.lsgscores.data.authuser.AppUserDaoSupabase,
    private val userDataPurger: fr.centuryspine.lsgscores.data.authuser.UserDataPurger
) : ViewModel() {

    // Linked player (if any) for the current authenticated user
    private val _linkedPlayerId = MutableStateFlow<Long?>(null)
    val linkedPlayerId: StateFlow<Long?> = _linkedPlayerId

    // Track manual sign-out to bypass debounce
    private val _signedOutManually = MutableStateFlow(false)
    val signedOutManually: StateFlow<Boolean> = _signedOutManually

    init {
        // Trace session status changes for debugging and ensure app_user row exists
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        Log.d("AuthVM", "SessionStatus=Authenticated userId=${status.session.user?.id}")
                        try {
                            appUserDao.ensureUserRow()
                            // Warm linked state as well
                            _linkedPlayerId.value = appUserDao.getLinkedPlayerId()
                        } catch (t: Throwable) {
                            Log.w("AuthVM", "ensureUserRow/getLinked failed: ${t.message}")
                        }
                        // Reset manual sign-out flag once authenticated again
                        _signedOutManually.value = false
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Log.d("AuthVM", "SessionStatus=NotAuthenticated")
                        _linkedPlayerId.value = null
                    }
                    is SessionStatus.LoadingFromStorage -> {
                        Log.d("AuthVM", "SessionStatus=LoadingFromStorage")
                    }
                    else -> {
                        Log.d("AuthVM", "SessionStatus=${status::class.simpleName}")
                    }
                }
            }
        }
    }

    // Expose the authenticated user (null if not authenticated)
    val user: StateFlow<UserInfo?> =
        supabase.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> status.session.user
                    else -> null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // UI-facing auth state with debounce on transient NotAuthenticated
    val authUiState: StateFlow<AuthUiState> =
        combine(supabase.auth.sessionStatus, signedOutManually) { status, signedOut ->
            Pair(status, signedOut)
        }
            .transformLatest { (status, signedOut) ->
                if (signedOut) {
                    emit(AuthUiState.NotAuthenticated)
                    return@transformLatest
                }
                when (status) {
                    is SessionStatus.Authenticated -> emit(AuthUiState.Authenticated)
                    is SessionStatus.LoadingFromStorage -> emit(AuthUiState.Checking)
                    is SessionStatus.NotAuthenticated -> {
                        // Grace window: emit Checking first, then NotAuthenticated if it persists
                        emit(AuthUiState.Checking)
                        // 2 seconds debounce; adjust if needed
                        delay(2000)
                        emit(AuthUiState.NotAuthenticated)
                    }
                    else -> emit(AuthUiState.Checking)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState.Checking)

    // Keep a simple boolean for legacy collectors that expect loading/authenticated
    val isAuthenticatedOrLoading: StateFlow<Boolean> =
        authUiState
            .map { it == AuthUiState.Authenticated || it == AuthUiState.Checking }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)


    fun signInWithGoogle() {
        Log.d("AuthVM", "signInWithGoogle() called")
        viewModelScope.launch {
            try {
                supabase.auth.signInWith(Google) { }
                Log.d("AuthVM", "signInWithGoogle() launched OAuth flow")
            } catch (t: Throwable) {
                Log.e("AuthVM", "signInWithGoogle() failed: ${t.message}", t)
            }
        }
    }

    fun signOut() {
        Log.d("AuthVM", "signOut() called")
        viewModelScope.launch {
            try {
                _signedOutManually.value = true
                supabase.auth.signOut()
                Log.d("AuthVM", "signOut() done")
            } catch (t: Throwable) {
                Log.e("AuthVM", "signOut() failed: ${t.message}", t)
            }
        }
    }

    // Delete account (purge user data and sign out). Emits state for UI.
    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState

    fun resetDeleteAccountState() { _deleteAccountState.value = DeleteAccountState.Idle }

    fun deleteAccount() {
        Log.d("AuthVM", "deleteAccount() called")
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            try {
                userDataPurger.purgeAllForCurrentUser()
                // After data purge, sign out the user from auth
                try { supabase.auth.signOut() } catch (_: Throwable) {}
                _deleteAccountState.value = DeleteAccountState.Success
            } catch (t: Throwable) {
                Log.e("AuthVM", "deleteAccount() failed: ${t.message}", t)
                _deleteAccountState.value = DeleteAccountState.Error(t.message)
            }
        }
    }
}

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()
    data object Loading : DeleteAccountState()
    data object Success : DeleteAccountState()
    data class Error(val message: String?) : DeleteAccountState()
}

sealed class AuthUiState {
    data object Authenticated : AuthUiState()
    data object Checking : AuthUiState()
    data object NotAuthenticated : AuthUiState()
}
