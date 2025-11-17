package fr.centuryspine.lsgscores.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import fr.centuryspine.lsgscores.viewmodel.AuthUiState
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun AuthGate(
    authViewModel: AuthViewModel = hiltViewModel(),
    appContent: @Composable () -> Unit
) {
    val user by authViewModel.user.collectAsStateWithLifecycle()
    val authUi by authViewModel.authUiState.collectAsStateWithLifecycle()
    val signedOut by authViewModel.signedOutManually.collectAsStateWithLifecycle()

    // Sticky: once we've seen a non-null user in this session, keep the app content visible
    // during transient restores/refreshes so we don't unmount the UI tree.
    var hadUser by rememberSaveable("auth_had_user") { mutableStateOf(false) }

    // Detect app resuming to foreground and give a brief grace window to avoid flashing Auth
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResuming by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isResuming = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isResuming) {
        if (isResuming) {
            // Small debounce to let Supabase restore session from storage
            delay(600)
            isResuming = false
        }
    }

    LaunchedEffect(user) {
        val uid = user?.id ?: "null"
        Log.d("AuthGate", "User state changed: $uid")
        if (user != null) hadUser = true
    }

    LaunchedEffect(signedOut) {
        if (signedOut) {
            // Clear sticky state on explicit sign-out so we show Auth immediately
            hadUser = false
        }
    }

    when {
        // Explicit manual sign-out: immediately show Auth screen regardless of transient states
        signedOut -> {
            Log.d("AuthGate", "Rendering AuthScreen after explicit sign-out")
            AuthScreen(
                onGoogle = { authViewModel.signInWithGoogle() },
                onFacebook = null
            )
        }
        // Keep app content mounted if authenticated OR if we previously had a user (sticky) during transient restores
        user != null || hadUser -> {
            Log.d("AuthGate", "Rendering app content (authenticated or sticky)")
            appContent()
        }
        // Before any user ever existed, show a loader during checking/resume
        authUi == AuthUiState.Checking || isResuming -> {
            Log.d("AuthGate", "Rendering Loading screen (initial session restore/resume)")
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            Log.d("AuthGate", "Rendering AuthScreen (no user)")
            AuthScreen(
                onGoogle = { authViewModel.signInWithGoogle() },
                onFacebook = null // Not yet implemented
            )
        }
    }
}
