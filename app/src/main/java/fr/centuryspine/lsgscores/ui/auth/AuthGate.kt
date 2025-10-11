package fr.centuryspine.lsgscores.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    LaunchedEffect(user) {
        Log.d("AuthGate", "User state changed: ${user?.id ?: "null"}")
    }

    when {
        user != null -> {
            Log.d("AuthGate", "Rendering app content (user authenticated)")
            appContent()
        }
        isAuthOrLoading -> {
            Log.d("AuthGate", "Rendering Loading screen (restoring session)")
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
