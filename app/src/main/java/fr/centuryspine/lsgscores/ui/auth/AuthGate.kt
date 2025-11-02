package fr.centuryspine.lsgscores.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel

@Composable
fun AuthGate(
    authViewModel: AuthViewModel = hiltViewModel(),
    appContent: @Composable () -> Unit
) {
    val user by authViewModel.user.collectAsState()
    val isAuthOrLoading by authViewModel.isAuthenticatedOrLoading.collectAsState()

    // Sticky: once we've seen a non-null user in this session, keep the app content visible
    // during transient restores/refreshes so we don't unmount the UI tree.
    var hadUser by rememberSaveable("auth_had_user") { mutableStateOf(false) }

    LaunchedEffect(user) {
        Log.d("AuthGate", "User state changed: ${user?.id ?: "null"}")
        if (user != null) hadUser = true
    }

    when {
        // If user is present, or we previously had a user and we're just restoring, keep app content
        user != null || (hadUser && isAuthOrLoading) -> {
            Log.d("AuthGate", "Rendering app content (authenticated or sticky during restore)")
            appContent()
        }
        // Only show a full-screen loading gate before we ever had a user
        isAuthOrLoading -> {
            Log.d("AuthGate", "Rendering Loading screen (initial session restore)")
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
