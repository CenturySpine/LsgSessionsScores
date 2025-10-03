package fr.centuryspine.lsgscores.ui.auth

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import fr.centuryspine.lsgscores.viewmodel.AuthViewModel

@Composable
fun AuthGate(
    authViewModel: AuthViewModel = hiltViewModel(),
    appContent: @Composable () -> Unit
) {
    val user by authViewModel.user.collectAsState()
    val isAuthOrLoading by authViewModel.isAuthenticatedOrLoading.collectAsState()

    LaunchedEffect(user) {
        Log.d("AuthGate", "User state changed: ${user?.id ?: "null"}")
    }

    if (!isAuthOrLoading) {
        Log.d("AuthGate", "Rendering AuthScreen (no user)")
        AuthScreen(
            onGoogle = { authViewModel.signInWithGoogle() },
            onFacebook = null // Not yet implemented
        )
    } else {
        Log.d("AuthGate", "Rendering app content (user authenticated or restoring)")
        appContent()
    }
}
