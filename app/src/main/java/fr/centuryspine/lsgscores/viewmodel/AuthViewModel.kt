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
    private val supabase: SupabaseClient
) : ViewModel() {

    init {
        // Trace session status changes for debugging
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        Log.d("AuthVM", "SessionStatus=Authenticated userId=${status.session.user?.id}")
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Log.d("AuthVM", "SessionStatus=NotAuthenticated")
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
}
