// ui/sessions/OngoingSessionScreen.kt

package com.example.lsgscores.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.ui.BottomNavItem

@Composable
fun OngoingSessionScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // --- Action buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    sessionViewModel.deleteOngoingSession {
                        // Force navigation to home and remove all previous destinations
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    sessionViewModel.validateOngoingSession {
                        // Force navigation to home and remove all previous destinations
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            ) {
                Text("Validate")
            }
        }

        // --- Rest of your UI to display session details goes here ---
        Text("Ongoing session details will be displayed here.")
    }
}
