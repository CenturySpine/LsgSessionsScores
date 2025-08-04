// ui/BottomNavItem.kt

package com.example.lsgscores.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    object NewSession : BottomNavItem("new_session", "New session", Icons.Outlined.PlayArrow)
    object Users : BottomNavItem("user_list", "Players", Icons.Filled.Person)
    object Holes : BottomNavItem("hole_list", "Holes", Icons.Filled.LocationOn)
}
