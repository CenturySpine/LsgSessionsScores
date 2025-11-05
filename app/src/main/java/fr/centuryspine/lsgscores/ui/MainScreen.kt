// ui/MainScreen.kt

package fr.centuryspine.lsgscores.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.areas.AreasScreen
import fr.centuryspine.lsgscores.ui.holes.HoleDetailScreen
import fr.centuryspine.lsgscores.ui.holes.HoleFormScreen
import fr.centuryspine.lsgscores.ui.holes.HoleListScreen
import fr.centuryspine.lsgscores.ui.home.HomeScreen
import fr.centuryspine.lsgscores.ui.players.PlayerDetailScreen
import fr.centuryspine.lsgscores.ui.players.PlayerFormScreen
import fr.centuryspine.lsgscores.ui.players.PlayerListScreen
import fr.centuryspine.lsgscores.ui.sessions.OngoingSessionScreen
import fr.centuryspine.lsgscores.ui.sessions.PlayedHoleScoreScreen
import fr.centuryspine.lsgscores.ui.sessions.SessionCreationScreen
import fr.centuryspine.lsgscores.ui.sessions.SessionHistoryScreen
import fr.centuryspine.lsgscores.ui.sessions.SessionTeamsScreen
import fr.centuryspine.lsgscores.ui.sessions.SessionQrScreen
import fr.centuryspine.lsgscores.ui.sessions.JoinSessionScannerScreen
import fr.centuryspine.lsgscores.ui.sessions.JoinSessionTeamPickerScreen
import fr.centuryspine.lsgscores.ui.settings.SettingsScreen
import fr.centuryspine.lsgscores.viewmodel.CityViewModel
import fr.centuryspine.lsgscores.viewmodel.GameZoneViewModel
import fr.centuryspine.lsgscores.viewmodel.HoleViewModel
import fr.centuryspine.lsgscores.viewmodel.LanguageViewModel
import fr.centuryspine.lsgscores.viewmodel.PlayerViewModel
import fr.centuryspine.lsgscores.viewmodel.SessionViewModel
import fr.centuryspine.lsgscores.viewmodel.ThemeViewModel
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
        currentRoute == DrawerNavItem.Areas.route ||
                currentRoute == DrawerNavItem.Players.route ||
                currentRoute == DrawerNavItem.Holes.route ||
                currentRoute == DrawerNavItem.SessionHistory.route ||
                currentRoute == DrawerNavItem.Settings.route ||
                currentRoute?.startsWith("user_detail/") == true ||
                currentRoute?.startsWith("hole_detail/") == true ||
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
    playerViewModel: PlayerViewModel = hiltViewModel(),
    holeViewModel: HoleViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
    languageViewModel: LanguageViewModel = hiltViewModel(),
    gameZoneViewModel: GameZoneViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    cityViewModel: CityViewModel = hiltViewModel()
) {
    val hasOngoingSessionForCurrentCity by sessionViewModel.hasOngoingSessionForCurrentCity.collectAsStateWithLifecycle()
    val isParticipant by sessionViewModel.isParticipantMode.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Add city check
    val hasCitySelected by cityViewModel.hasCitySelected.collectAsStateWithLifecycle()  // lifecycle-aware
    var showNoCityAlert by remember { mutableStateOf(false) }  // Add this

    // Bottom bar items (only 3 now)
    val bottomItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.NewSession,
        BottomNavItem.OngoingSession
    )

    // Drawer items

    val drawerMainItems = listOf(
        DrawerNavItem.Areas,
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
                                    (item == DrawerNavItem.Holes && currentRoute?.startsWith("hole_detail/") == true) ||
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
                            if (!hasCitySelected) {
                                showNoCityAlert = true
                            } else {
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
                                DrawerNavItem.Areas.route -> stringResource(R.string.main_topbar_title_areas)
                                DrawerNavItem.Players.route -> stringResource(R.string.main_topbar_title_players)
                                DrawerNavItem.Holes.route -> stringResource(R.string.main_topbar_title_holes)
                                DrawerNavItem.SessionHistory.route -> stringResource(R.string.main_topbar_title_session_history)
                                DrawerNavItem.Settings.route -> stringResource(R.string.main_topbar_title_settings)
                                "add_user" -> stringResource(R.string.main_topbar_title_add_player)
                                "add_hole" -> stringResource(R.string.main_topbar_title_add_hole)
                                "new_session_teams" -> stringResource(R.string.main_topbar_title_select_teams)
                                "user_detail/{userId}" -> stringResource(R.string.main_topbar_title_player_details)
                                "hole_detail/{holeId}" -> stringResource(R.string.main_topbar_title_hole_details)
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
                            is BottomNavItem.NewSession -> !hasOngoingSessionForCurrentCity
                            is BottomNavItem.OngoingSession -> hasOngoingSessionForCurrentCity
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
                                    if (item == BottomNavItem.NewSession && !hasCitySelected) {
                                        showNoCityAlert = true
                                    } else {
                                        // For NewSession, force navigation without launchSingleTop to avoid caching issues
                                        if (item == BottomNavItem.NewSession) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                // Remove launchSingleTop to force recreation
                                                restoreState = false // Also disable state restoration
                                            }
                                        } else {
                                            if (item == BottomNavItem.Home) {
                                                navController.navigate(item.route) {
                                                    // Navigate to Home and clear up to start destination to avoid ghost back navigation
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            } else {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
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
                    HomeScreen(cityViewModel = cityViewModel, onJoinSessionClick = {
                        navController.navigate("join_session_scan")
                    })
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
                    route = "hole_detail/{holeId}",
                    arguments = listOf(navArgument("holeId") { type = NavType.LongType })
                ) {
                    val holeId = it.arguments?.getLong("holeId")
                    HoleDetailScreen(
                        navController = navController,
                        holeId = holeId
                    )
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
                composable(DrawerNavItem.Areas.route) {
                    AreasScreen(gameZoneViewModel, cityViewModel)
                }

                // QR and Join routes
                composable("session_qr") {
                    SessionQrScreen(navController, sessionViewModel)
                }
                composable("join_session_scan") {
                    JoinSessionScannerScreen(navController)
                }
                composable(
                    route = "join_session_pick_team/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                    JoinSessionTeamPickerScreen(
                        navController = navController,
                        sessionViewModel = sessionViewModel,
                        sessionId = sessionId
                    )
                }
            }
        }
    }
    // No city selected alert dialog
    if (showNoCityAlert) {
        AlertDialog(
            onDismissRequest = { showNoCityAlert = false },
            title = { Text(stringResource(R.string.no_city_alert_select_a_city)) },
            text = { Text(stringResource(R.string.no_city_alert_you_must_select_a_city)) },
            confirmButton = {
                TextButton(onClick = { showNoCityAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}
