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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val appUserDao: fr.centuryspine.lsgscores.data.authuser.AppUserDaoSupabase,
    private val userDataPurger: fr.centuryspine.lsgscores.data.authuser.UserDataPurger
) : ViewModel() {

    // Linked player (if any) for the current authenticated user
    private val _linkedPlayerId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val linkedPlayerId: StateFlow<Long?> = _linkedPlayerId

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

    // Keep rendering app content while session restores from storage to avoid UI teardown
    val isAuthenticatedOrLoading: StateFlow<Boolean> =
        supabase.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> true
                    is SessionStatus.LoadingFromStorage -> true
                    else -> false
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun linkCurrentUserToPlayer(playerId: Long) {
        viewModelScope.launch {
            try {
                val ok = appUserDao.linkToPlayer(playerId)
                if (ok) {
                    _linkedPlayerId.value = playerId
                }
            } catch (t: Throwable) {
                Log.w("AuthVM", "linkCurrentUserToPlayer failed: ${t.message}")
            }
        }
    }

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
                supabase.auth.signOut()
                Log.d("AuthVM", "signOut() done")
            } catch (t: Throwable) {
                Log.e("AuthVM", "signOut() failed: ${t.message}", t)
            }
        }
    }

    // Delete account (purge user data and sign out). Emits state for UI.
    private val _deleteAccountState = kotlinx.coroutines.flow.MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
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
