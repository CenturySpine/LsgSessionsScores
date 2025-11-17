package fr.centuryspine.lsgscores.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.ui.graphics.vector.ImageVector
import fr.centuryspine.lsgscores.R

sealed class DrawerNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    object Players : DrawerNavItem(
        "user_list",
        R.string.drawer_nav_players,
        Icons.Filled.Person
    )

    object Holes : DrawerNavItem(
        "hole_list",
        R.string.drawer_nav_holes,
        Icons.Rounded.GolfCourse
    )

    object SessionHistory : DrawerNavItem(
        "session_history",
        R.string.drawer_nav_session_history,
        Icons.Filled.History
    )

    object Settings : DrawerNavItem(
        "settings",
        R.string.drawer_nav_settings,
        Icons.Filled.Settings
    )

    object Areas : DrawerNavItem(
        "areas",
        R.string.drawer_nav_areas,
        Icons.Filled.LocationOn
    )
}