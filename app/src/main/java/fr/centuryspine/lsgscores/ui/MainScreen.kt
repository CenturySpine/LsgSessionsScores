// ui/MainScreen.kt

package fr.centuryspine.lsgscores.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import fr.centuryspine.lsgscores.R
import fr.centuryspine.lsgscores.ui.areas.AreasScreen
import fr.centuryspine.lsgscores.ui.common.RemoteImage
import fr.centuryspine.lsgscores.ui.holes.HoleDetailScreen
import fr.centuryspine.lsgscores.ui.holes.HoleFormScreen
import fr.centuryspine.lsgscores.ui.holes.HoleListScreen
import fr.centuryspine.lsgscores.ui.home.HomeScreen
import fr.centuryspine.lsgscores.ui.players.PlayerDetailScreen
import fr.centuryspine.lsgscores.ui.players.PlayerListScreen
import fr.centuryspine.lsgscores.ui.sessions.*
import fr.centuryspine.lsgscores.ui.settings.SettingsScreen
import fr.centuryspine.lsgscores.utils.AppVersionResolver
import fr.centuryspine.lsgscores.viewmodel.*
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
                currentRoute == "add_hole" -> NavigationContext.DRAWER

        // Other routes
        else -> NavigationContext.OTHER
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    io.github.jan.supabase.annotations.SupabaseExperimental::class
)
@Composable
fun MainScreen(
    navController: NavHostController,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    holeViewModel: HoleViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
    languageViewModel: LanguageViewModel = hiltViewModel(),
    gameZoneViewModel: GameZoneViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    cityViewModel: CityViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
    currentUserProvider: fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
) {
    val hasOngoingSessionForCurrentCity by sessionViewModel.hasOngoingSessionForCurrentCity.collectAsStateWithLifecycle()
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

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                    ) {
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

                    // version information (anchored at bottom)
                    val context = LocalContext.current
                    val localVersionName by remember(context) {
                        mutableStateOf(AppVersionResolver.resolveLocalVersionName(context))
                    }
                    Text(
                        text = localVersionName,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        fontSize = 10.sp
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

                    // Id du joueur lié à l'utilisateur courant (réactif via AuthViewModel)
                    val linkedPlayerId by authViewModel.linkedPlayerId.collectAsStateWithLifecycle()
                    // Observer la liste des joueurs et extraire le joueur lié (réactif aux changements)
                    val allPlayers by playerViewModel.players.collectAsStateWithLifecycle(initialValue = emptyList())
                    val linkedPlayer = remember(linkedPlayerId, allPlayers) {
                        linkedPlayerId?.let { id -> allPlayers.find { it.id == id } }
                    }

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
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            enabled = isEnabled
                        )
                    }

                    NavigationBarItem(
                        icon = {
                            val p = linkedPlayer
                            if (p?.photoUri?.isNotBlank() == true) {
                                RemoteImage(
                                    url = p.photoUri,
                                    contentDescription = "Photo ${'$'}{p.name}",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            linkedPlayerId?.let { id ->
                                navController.navigate("user_detail/${id}") {
                                    launchSingleTop = true
                                }
                            }
                        },
                        label = { Text("Profile") },
                        enabled = linkedPlayerId != null
                    )
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
                    HoleListScreen(navController, holeViewModel, currentUserProvider)
                }
                composable(DrawerNavItem.SessionHistory.route) {
                    SessionHistoryScreen(sessionViewModel)
                }

                // Other routes (unchanged)
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
                        holeId = holeId,
                        holeViewModel,
                        currentUserProvider
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
                    // Render Settings as usual
                    SettingsScreen(themeViewModel, languageViewModel, authViewModel)
                    // Inline debug entry: small, non-intrusive section at bottom
//                    androidx.compose.material3.Button(onClick = { navController.navigate("debug_realtime") }, modifier = Modifier.padding(16.dp)) {
//                        Text("Open Realtime Debug")
//                    }
                }

                composable(DrawerNavItem.Areas.route) {
                    AreasScreen(gameZoneViewModel, cityViewModel, currentUserProvider)
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
                        authViewModel = authViewModel,
                        sessionId = sessionId
                    )
                }
            }
        }
    }

    fun closeCityAlert(): () -> Unit = { showNoCityAlert = false }

    // No city-selected alert dialog
    if (showNoCityAlert) {
        AlertDialog(
            onDismissRequest = closeCityAlert(),
            title = { Text(stringResource(R.string.no_city_alert_select_a_city)) },
            text = { Text(stringResource(R.string.no_city_alert_you_must_select_a_city)) },
            confirmButton = {
                TextButton(onClick = closeCityAlert()) {
                    Text("OK")
                }
            }
        )
    }
}
