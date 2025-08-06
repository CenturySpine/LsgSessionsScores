// ui/BottomNavItem.kt

package com.example.lsgscores.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.ui.graphics.vector.ImageVector


sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    object NewSession : BottomNavItem("new_session", "New", Icons.Outlined.PlayArrow)

    object OngoingSession : BottomNavItem("ongoing_session", "Ongoing", Icons.Rounded.CalendarToday)
    object Users : BottomNavItem("user_list", "Players", Icons.Filled.Person)
    object Holes : BottomNavItem("hole_list", "Holes", Icons.Rounded.GolfCourse)
}
