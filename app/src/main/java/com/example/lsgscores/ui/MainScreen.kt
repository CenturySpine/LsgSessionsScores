// ui/MainScreen.kt

package com.example.lsgscores.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.lsgscores.ui.holes.HoleFormScreen
import com.example.lsgscores.ui.players.PlayerListScreen
import com.example.lsgscores.ui.holes.HoleListScreen
import com.example.lsgscores.ui.home.HomeScreen
import com.example.lsgscores.ui.sessions.OngoingSessionScreen
import com.example.lsgscores.ui.sessions.SessionCreationScreen
import com.example.lsgscores.ui.sessions.SessionTeamsScreen
import com.example.lsgscores.ui.players.PlayerDetailScreen
import com.example.lsgscores.ui.players.PlayerFormScreen
import com.example.lsgscores.ui.sessions.PlayedHoleScoreScreen
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    holeViewModel: HoleViewModel,
    sessionViewModel: SessionViewModel
) {

    val ongoingSession by sessionViewModel.ongoingSession.collectAsState(initial = null)

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.NewSession,
        BottomNavItem.OngoingSession,
        BottomNavItem.Users,
        BottomNavItem.Holes
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val isEnabled = when (item) {
                        is BottomNavItem.NewSession -> ongoingSession == null
                        is BottomNavItem.OngoingSession -> ongoingSession != null
                        else -> true
                    }
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = navController.currentDestination?.route == item.route,
                        onClick = {
                            if (isEnabled) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        enabled = isEnabled
                    )
                }
            }
        }

    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Users.route) {
                PlayerListScreen(navController, playerViewModel)
            }
            composable(BottomNavItem.NewSession.route) {
                // Only pass the sessionViewModel, SessionCreationScreen will handle its own logic
                SessionCreationScreen(navController, sessionViewModel)
            }
            composable("new_session_teams") {
                SessionTeamsScreen(navController, sessionViewModel, playerViewModel)
            }
            composable(BottomNavItem.OngoingSession.route) {
                OngoingSessionScreen(navController, sessionViewModel, holeViewModel)
            }
            composable("add_user") {
                PlayerFormScreen(navController, playerViewModel)
            }
            composable(BottomNavItem.Holes.route) {
                HoleListScreen(navController, holeViewModel)
            }
            composable("add_hole") {
                HoleFormScreen(navController, holeViewModel)
            }
            // Add this composable for the user detail screen
            composable(
                route = "user_detail/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) {
                val userId = it.arguments?.getLong("userId")
                if (userId != null) {
                    PlayerDetailScreen(
                        navController = navController,
                        userId = userId,
                        playerViewModel = playerViewModel
                    )
                }
            }
            composable(
                route = "played_hole_score/{playedHoleId}",
                arguments = listOf(navArgument("playedHoleId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playedHoleId = backStackEntry.arguments?.getLong("playedHoleId") ?: 0L
                PlayedHoleScoreScreen(
                    navController = navController,
                    sessionViewModel = sessionViewModel,
                    playedHoleId = playedHoleId
                )
            }


        }
    }
}
