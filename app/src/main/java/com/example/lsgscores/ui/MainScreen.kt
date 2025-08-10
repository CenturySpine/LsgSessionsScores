// ui/MainScreen.kt

package com.example.lsgscores.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.lsgscores.ui.holes.HoleFormScreen
import com.example.lsgscores.ui.holes.HoleListScreen
import com.example.lsgscores.ui.home.HomeScreen
import com.example.lsgscores.ui.players.PlayerDetailScreen
import com.example.lsgscores.ui.players.PlayerFormScreen
import com.example.lsgscores.ui.players.PlayerListScreen
import com.example.lsgscores.ui.sessions.OngoingSessionScreen
import com.example.lsgscores.ui.sessions.PlayedHoleScoreScreen
import com.example.lsgscores.ui.sessions.SessionCreationScreen
import com.example.lsgscores.ui.sessions.SessionHistoryScreen
import com.example.lsgscores.ui.sessions.SessionTeamsScreen
import com.example.lsgscores.ui.settings.SettingsScreen
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    holeViewModel: HoleViewModel,
    sessionViewModel: SessionViewModel,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val ongoingSession by sessionViewModel.ongoingSession.collectAsState(initial = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Bottom bar items (only 3 now)
    val bottomItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.NewSession,
        BottomNavItem.OngoingSession
    )

    // Drawer items

    val drawerMainItems = listOf(
        DrawerNavItem.Players,
        DrawerNavItem.Holes,
        DrawerNavItem.SessionHistory
    )

    val drawerSystemItems = listOf(
        DrawerNavItem.Settings
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Drawer header
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LSG Scores",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Drawer items
// Drawer main items
                drawerMainItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = navController.currentDestination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

// Separator
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

// Drawer system items
                drawerSystemItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = navController.currentDestination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        Text(
                            when (currentRoute) {
                                BottomNavItem.Home.route -> "LSG Scores"
                                BottomNavItem.NewSession.route -> "New Session"
                                BottomNavItem.OngoingSession.route -> "Ongoing Session"
                                DrawerNavItem.Players.route -> "Players"
                                DrawerNavItem.Holes.route -> "Holes"
                                DrawerNavItem.SessionHistory.route -> "Sessions History"
                                DrawerNavItem.Settings.route -> "Settings"
                                "add_user" -> "Add Player"
                                "add_hole" -> "Add Hole"
                                "new_session_teams" -> "Select Teams"
                                else -> "LSG Scores"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomItems.forEach { item ->
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
                // Existing routes...
                composable(BottomNavItem.Home.route) {
                    HomeScreen()
                }
                composable(BottomNavItem.NewSession.route) {
                    SessionCreationScreen(navController, sessionViewModel)
                }
                composable("new_session_teams") {
                    SessionTeamsScreen(navController, sessionViewModel, playerViewModel)
                }
                composable(BottomNavItem.OngoingSession.route) {
                    OngoingSessionScreen(navController, sessionViewModel, holeViewModel)
                }

                // Drawer routes
                composable(DrawerNavItem.Players.route) {
                    PlayerListScreen(navController, playerViewModel)
                }
                composable(DrawerNavItem.Holes.route) {
                    HoleListScreen(navController, holeViewModel)
                }
                composable(DrawerNavItem.SessionHistory.route) {
                    SessionHistoryScreen(navController, sessionViewModel)
                }

                // Other routes (unchanged)
                composable("add_user") {
                    PlayerFormScreen(navController, playerViewModel)
                }
                composable("add_hole") {
                    HoleFormScreen(navController, holeViewModel)
                }
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
                composable(DrawerNavItem.Settings.route) {
                    SettingsScreen(navController, themeViewModel)
                }
            }
        }
    }
}