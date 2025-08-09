package com.example.lsgscores.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.ui.graphics.vector.ImageVector

sealed class DrawerNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Players : DrawerNavItem(
        "user_list",
        "Players",
        Icons.Filled.Person
    )
    object Holes : DrawerNavItem(
        "hole_list",
        "Holes",
        Icons.Rounded.GolfCourse
    )
    object SessionHistory : DrawerNavItem(
        "session_history",
        "Sessions History",
        Icons.Filled.History
    )
}