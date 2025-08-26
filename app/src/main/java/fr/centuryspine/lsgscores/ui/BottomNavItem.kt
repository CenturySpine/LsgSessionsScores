package fr.centuryspine.lsgscores.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.ui.graphics.vector.ImageVector
import fr.centuryspine.lsgscores.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", R.string.bottom_nav_home, Icons.Filled.Home)
    object NewSession : BottomNavItem("new_session", R.string.bottom_nav_new, Icons.Outlined.PlayArrow)
    object OngoingSession : BottomNavItem("ongoing_session", R.string.bottom_nav_ongoing, Icons.Rounded.CalendarToday)
}