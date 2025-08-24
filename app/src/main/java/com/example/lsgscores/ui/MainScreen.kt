// ui/MainScreen.kt

package com.example.lsgscores.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.lsgscores.R
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
import com.example.lsgscores.viewmodel.GameZoneViewModel
import com.example.lsgscores.viewmodel.HoleViewModel
import com.example.lsgscores.viewmodel.LanguageViewModel
import com.example.lsgscores.viewmodel.PlayerViewModel
import com.example.lsgscores.viewmodel.SessionViewModel
import com.example.lsgscores.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@Composable
private fun getCurrentNavigationContext(currentRoute: String?): NavigationContext {
    return when {
        // Bottom bar routes (including dynamic routes)
        currentRoute == BottomNavItem.Home.route ||
                currentRoute == BottomNavItem.NewSession.route ||
                currentRoute == BottomNavItem.OngoingSession.route ||
                currentRoute?.startsWith("played_hole_score/") == true -> NavigationContext.BOTTOM_BAR

        // Drawer routes (including dynamic routes)
        currentRoute == DrawerNavItem.Players.route ||
                currentRoute == DrawerNavItem.Holes.route ||
                currentRoute == DrawerNavItem.SessionHistory.route ||
                currentRoute == DrawerNavItem.Settings.route ||
                currentRoute?.startsWith("user_detail/") == true ||
                currentRoute == "add_user" ||
                currentRoute == "add_hole" -> NavigationContext.DRAWER

        // Other routes
        else -> NavigationContext.OTHER
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    holeViewModel: HoleViewModel,
    sessionViewModel: SessionViewModel,
    languageViewModel: LanguageViewModel,
    gameZoneViewModel: GameZoneViewModel,
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

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val navigationContext = getCurrentNavigationContext(currentRoute)

                // Drawer header
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.main_drawer_header),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                drawerMainItems.forEach { item ->
                    val isSelected = navigationContext == NavigationContext.DRAWER && (
                            currentRoute == item.route ||
                                    // Handle dynamic routes for drawer items
                                    (item == DrawerNavItem.Players && currentRoute?.startsWith("user_detail/") == true) ||
                                    (item == DrawerNavItem.Players && currentRoute == "add_user") ||
                                    (item == DrawerNavItem.Holes && currentRoute == "add_hole")
                            )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        selected = isSelected,
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
                    val isSelected = navigationContext == NavigationContext.DRAWER &&
                            currentRoute == item.route

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        selected = isSelected,
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
                                BottomNavItem.Home.route -> stringResource(R.string.main_app_title)
                                BottomNavItem.NewSession.route -> stringResource(R.string.main_topbar_title_new_session)
                                BottomNavItem.OngoingSession.route -> stringResource(R.string.main_topbar_title_ongoing_session)
                                DrawerNavItem.Players.route -> stringResource(R.string.main_topbar_title_players)
                                DrawerNavItem.Holes.route -> stringResource(R.string.main_topbar_title_holes)
                                DrawerNavItem.SessionHistory.route -> stringResource(R.string.main_topbar_title_session_history)
                                DrawerNavItem.Settings.route -> stringResource(R.string.main_topbar_title_settings)
                                "add_user" -> stringResource(R.string.main_topbar_title_add_player)
                                "add_hole" -> stringResource(R.string.main_topbar_title_add_hole)
                                "new_session_teams" -> stringResource(R.string.main_topbar_title_select_teams)
                                "user_detail/{userId}" -> stringResource(R.string.main_topbar_title_player_details)
                                else -> stringResource(R.string.main_app_title)
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
                                contentDescription = stringResource(R.string.main_menu_icon_description)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val navigationContext = getCurrentNavigationContext(currentRoute)

                    bottomItems.forEach { item ->
                        val isEnabled = when (item) {
                            is BottomNavItem.NewSession -> ongoingSession == null
                            is BottomNavItem.OngoingSession -> ongoingSession != null
                            else -> true
                        }

                        val isSelected = navigationContext == NavigationContext.BOTTOM_BAR &&
                                currentRoute == item.route

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = isSelected,
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
                    SessionHistoryScreen(sessionViewModel)
                }

                // Other routes (unchanged)
                composable("add_user") {
                    PlayerFormScreen(navController, playerViewModel)
                }
                composable("add_hole") {
                    HoleFormScreen(navController, holeViewModel, gameZoneViewModel)
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
                    SettingsScreen(themeViewModel, languageViewModel)
                }
            }
        }
    }
}